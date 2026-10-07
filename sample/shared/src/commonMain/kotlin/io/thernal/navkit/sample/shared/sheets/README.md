# Bottom sheets — a second surface, one back stack

Catalog group **Bottom sheets**: [Share sheet](#simple-share-sheet) (simple) and
[Payment method](#advanced-payment-method) (advanced).

A sheet is not a different kind of navigation. It is a route on the same back stack, pushed with the
same `push`, closed by the same back — rendered on a different surface because its entry carries one
metadata key. Everything the other examples teach keeps working inside one.

| File | What is in it |
|---|---|
| [`SheetsRoutes.kt`](SheetsRoutes.kt) | the routes, the `SheetStepRoute` marker, and the two result keys |
| [`CloseSheet.kt`](CloseSheet.kt) | closing a whole run from inside a step |
| [`ShareSheetScreens.kt`](ShareSheetScreens.kt) | a screen opens one sheet and takes a value back from it |
| [`PaymentSheetScreens.kt`](PaymentSheetScreens.kt) | a sheet pushes a sheet, twice, and closes all of it at once |
| [`SheetsBindings.kt`](SheetsBindings.kt) | screens and catalog entries — `navEntry` beside `bottomSheetEntry` |

## The one line that makes a sheet

```kotlin
navEntry<PostRoute> { PostScreen() }
bottomSheetEntry<ShareSheetRoute> { ShareSheet() }
```

`bottomSheetEntry` is a thin wrapper over `entry` that adds `BOTTOM_SHEET_METADATA_KEY`. The host's
bottom-sheet scene claims entries carrying it and renders them as an overlay; everything else falls
through to the single-pane scene. The route, the push, the stack and the guards are unchanged —
which is why there is no `navigator.openSheet`, and why a deep link or a guard can land on a sheet
without knowing it is one.

## What the kit does not draw

The scene draws nothing. No scrim, no drag handle, no rounded corners, no outside-tap dismiss: those
are design-system decisions, and a navigation library that made them would be one an app has to
fight. [`ui/SheetSurface.kt`](../ui/SheetSurface.kt) is the sample supplying them — with the theme,
not beside one feature's screens, because it is the surface for *every* sheet in the app.

It is installed once, on the root host:

```kotlin
NavigationHostParams(…, bottomSheetContainer = SampleSheetContainer)
```

Once, not per step, and that is the whole reason a step can give way to the next with an animation:
the steps swap **inside** a panel that stays, so its height follows them. A surface each step
brought with it would be torn down and rebuilt, with nothing left to animate between.

The scene keeps opening and closing for itself, because an app cannot do those from the outside: an
overlay's exit has to finish before Navigation3 takes it out of composition. It wraps the sheet in
an `AnimatedVisibility` that fades it and hands the surface that scope, so the panel — the part that
should also move — says so itself:

```kotlin
Surface(
    modifier = Modifier.animateEnterExit(
        enter = slideInVertically { height -> height },
        exit = slideOutVertically { height -> height },
    ),
)
```

That is the whole slide-up. The scrim needs nothing: it is inside the same `AnimatedVisibility` and
the fade already covers it.

Two more things the surface has to get right, both consequences of how an overlay is rendered:

- **It fills the window.** The overlay is drawn as a sibling of the pane, not inside it, so a panel
  that measured only as tall as itself would end up against the top of the screen. `SheetSurface` is
  a `fillMaxSize` box that puts the panel at the bottom itself.
- **The panel has to consume taps.** A plain `Surface` does not, so without a `pointerInput` on it
  every tap meant for the sheet would reach the scrim behind and dismiss it.

## Two ways to close

| | From | Closes |
|---|---|---|
| `dismiss()` | the surface | the whole run — the scene knows how deep it is |
| `navigator.closeSheet()` | a step | the whole run — `popBackTo { it !is SheetStepRoute }` |

They do the same thing from opposite sides. The surface is written against no feature, so it cannot
ask whether a route is a step of a sheet and the kit answers for it. A step, whose routes are its
own package's, says it plainly with the marker interface.

## Simple: Share sheet

A post, a Share button, three targets. The sheet posts a typed result and pops:

```kotlin
ListRow(
    title = target.name,
    onClick = {
        results.post(key = SharedWith, value = target.name)
        navigator.popBack()
    },
)
```

What to notice:

- **The screen underneath stays composed.** An overlay covers it without removing it, so
  `ResultEffect` re-runs on the *post* rather than on the return: the "Shared with" row changes
  while the sheet is still open. A pushed full screen delivers the same value a moment later, when
  it is uncovered. It is the one behavioural difference between the two surfaces that a screen can
  actually observe.
- **Back closes it**, because the sheet is the top of the stack and nothing special is registered.
- **Its entries are capped at `STARTED`** while a sheet is over them — Navigation3 does not resume a
  pane under an overlay, which matters for anything keyed on `RESUMED`.

**Try it:** open *Share sheet*, tap *Share*, and watch the panel rise while the scrim fades in behind
it. Then tap *Messages*: the row behind the panel changes before the panel has finished sliding away.

## Advanced: Payment method

Three sheet routes — *Choose a method* → *Add a card* → *Confirm* — pushed one on top of the other
with the ordinary `push`. The scene claims the whole **run** of consecutive sheet entries, so they
are one panel with a back stack of its own: back returns to the previous step, and only back on the
first step closes the sheet.

Closing from anywhere in the run is one command:

```kotlin
internal fun Navigator.closeSheet() {
    popBackTo { route -> route !is SheetStepRoute }
}
```

What to notice:

- **The marker interface is what makes that one line possible.** A sheet that opened two more is
  three entries, and the tap outside means all of them. `SheetStepRoute` lets the predicate say
  "down to the first route that is not a sheet step" without naming a step or counting them.
- **A sheet step is an ordinary route**, so `ConfirmCardSheetRoute(maskedNumber)` is the ordinary
  way to hand a value forwards — and the last step posts one result backwards for the whole run.
- **Nothing coordinates the run.** No sheet state, no `sheetNavigator`: the steps are pushed like
  any other route and the scene groups them because they are adjacent in the stack.
- **A non-sheet route pushed on top ends the run.** The top entry stops being a sheet, the scene
  stops claiming it, and the sheet is gone until those entries are popped — which is what a "see
  full terms" link out of a sheet should do.
- **The panel's height animates between steps.** *Add a card* is taller than *Choose a method*, and
  the panel grows into it while the two steps slide and fade past each other. Nothing in the example
  asks for that: the steps are swapped inside one surface with `AnimatedContent`, whose size
  transform is already the panel's height following whatever step is in it.
- **Pushing a step does not reopen the sheet.** One surface means one scene, kept across the stack
  change, so the panel that is already up stays up and the open animation does not play again.

**Try it:** open *Payment method*, tap the payment row, *Add a card*, pick a brand, then press system
back — you are on *Add a card*, not back in the basket. Press it again for the list. Go forward to
*Confirm* and tap outside instead: all three steps close at once, and the basket row already shows
the card.

## Doing this in your app

1. Register the route with `bottomSheetEntry` instead of `navEntry`. Nothing else changes.
2. Draw the surface once, in your design system, and install it as `bottomSheetContainer`. Move the
   panel with `animateEnterExit`; the scene's fade already covers everything else.
3. Give the sheet routes of a flow a marker interface, and write dismissal as
   `popBackTo { it !is YourSheetRoute }` so it does not depend on how deep the user went.
4. Push further steps with `push`; they join the same panel as long as they are sheets too.
5. Hand values forwards with route properties and backwards with a result key, exactly as for full
   screens.

## Pitfalls

- **A surface that wraps its content.** The overlay is laid out against the top of the screen unless
  something fills the window and aligns the panel itself.
- **A surface per step instead of per host.** It works, and it costs the height animation: there is
  nothing left standing between one step and the next.
- **A scrim that does not consume taps**, or a panel that does — the first closes the sheet on every
  tap inside it, the second makes the outside tap dead.
- **`popBack()` for dismissal in a multi-step sheet.** It removes one step; the user meant the sheet.
- **Assuming the screen underneath is paused.** It is composed and recomposing, capped at `STARTED`.
- **Expecting a sheet to be dismissed by process death.** Its routes are restored with the rest of
  the stack, so the sheet reopens — make it a `TransientRoute` if that is wrong for your flow.

## Read more

- [Bottom sheets, scenes and transitions](../../../../../../../../../../navigation/api/README.md#bottom-sheets-scenes-and-transitions)
  in the API guide
- [Results](../../../../../../../../../../navigation/api/README.md#results) — the mailbox both examples use
- [`results`](../results/README.md) — the same value-backwards mechanism on full screens
- [All examples](../../../../../../../../../README.md#the-examples)
