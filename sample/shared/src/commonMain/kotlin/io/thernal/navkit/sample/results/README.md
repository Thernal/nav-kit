# Results — a value travelling backwards

Catalog group **Results**: [Pick a colour](#simple-pick-a-colour) (simple) and
[Review request](#real-life-review-request) (real life).

A screen closes and leaves a value for a screen that is already on the stack and about to be
uncovered: a picker, a confirmation, a multi-step flow that ends in a decision. The producer is
gone by the time the consumer reads, so the value waits in a mailbox — `NavigationResults`.

| File | What is in it |
|---|---|
| [`ResultsRoutes.kt`](ResultsRoutes.kt) | the routes, the `ReviewDecision` value, and the two typed keys |
| [`PickerScreens.kt`](PickerScreens.kt) | a picker posts, the screen behind it consumes |
| [`ReviewScreens.kt`](ReviewScreens.kt) | a three-step flow posts one decision and leaves in one command |
| [`ReviewHomeViewModel.kt`](ReviewHomeViewModel.kt) | where the returning decision lands |
| [`ResultsBindings.kt`](ResultsBindings.kt) | screens and catalog entries |

## Result or argument?

| | Result | Argument |
|---|---|---|
| Direction | a closing screen → a screen already on the stack | an opening screen → screens about to be pushed |
| Who waits | the producer is gone, the consumer stays | the consumer does not exist yet |
| Lifetime | one delivery | as long as the scope's routes are on the stack |
| Example | this package | [`arguments`](../arguments/README.md) |

## The key

A key is declared once, next to the routes of the feature that produces the result, and both sides
use it — so a producer and a consumer cannot disagree about the type without the compiler saying so.

```kotlin
val SelectedColour = resultKey<String>("results.selected_colour")

data class ReviewDecision(val isApproved: Boolean, val note: String)
val ReviewOutcome = resultKey<ReviewDecision>("results.review_outcome")
```

Prefix the name with the feature. Two features declaring the same name with different types fail
loudly when read, instead of answering `null`.

## Simple: Pick a colour

The picker posts, then pops. It never learns who consumed the value:

```kotlin
@Composable
fun PickerScreen() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current
    listOf("Teal", "Amber", "Indigo").forEach { colour ->
        ExampleAction(
            label = colour,
            onClick = {
                results.post(key = SelectedColour, value = colour)
                navigator.popBack()
            },
        )
    }
}
```

The screen behind it consumes, once:

```kotlin
@Composable
fun PickerHomeScreen() {
    val navigator = LocalNavigator.current
    var colour by rememberSaveable { mutableStateOf<String?>(null) }

    ResultEffect(SelectedColour) { picked -> colour = picked }

    ExampleReadout(label = "Selected", value = colour ?: "nothing yet")
    ExampleAction(label = "Open the picker", onClick = { navigator.push(PickerRoute) })
}
```

What to notice:

- **`ResultEffect` runs when this screen is composed.** Navigation3 composes only the current scene,
  so while the picker covers this screen the effect does not run; it runs when the user comes back,
  which is exactly when the result is wanted.
- **`rememberSaveable`, not `remember`.** This screen leaves composition every time the picker covers
  it. A plain `remember` forgot the last pick each time the picker was opened again.
- **Consuming removes the value** — one delivery, never two.

**Try it:** open *Pick a colour*, pick Teal, open the picker again and pick Amber. The readout keeps
the last pick in between.

## Real life: Review request

A whole flow is launched, walks three screens, and hands one value back to the screen that launched
it — by then several entries down the stack.

The last step posts and leaves the whole flow in one command:

```kotlin
private fun finish(approved: Boolean, results: NavigationResults, navigator: Navigator) {
    val note = if (approved) "looks good" else "needs changes"
    results.post(key = ReviewOutcome, value = ReviewDecision(isApproved = approved, note = note))
    navigator.popBackTo { candidate -> candidate is ReviewHomeRoute }
}
```

The launcher forwards the value to its state holder, which decides:

```kotlin
@Composable
fun ReviewHomeScreen() {
    val results = LocalNavigationResults.current
    val model: ReviewHomeViewModel = viewModel { ReviewHomeViewModel() }
    val decision by model.decision.collectAsState()

    ResultEffect(key = ReviewOutcome, onResult = model::onDecision)
    // …
    ExampleAction(
        label = "Clear the decision",
        onClick = {
            results.clear(ReviewOutcome)
            model.clear()
        },
    )
}
```

What to notice:

- **The decision lives in an entry-scoped ViewModel.** `viewModel { }` inside an entry is scoped to
  that entry by the host's ViewModel-store decorator, so it lives exactly as long as the launcher is
  on the stack — through the whole flow covering it.
- **`popBackTo` after `post`** uncovers the launcher with the answer already waiting. `popBackTo` is a
  jump, so a back interceptor inside the flow does not get a say.
- **`pending` exposes names, never values**, so one feature's results are not readable by every
  screen. The screen shows two readouts — what was pending when it came back, and what is pending
  now — because while a result is pending, the launcher is covered and not composed: the name can only
  be seen at the moment it returns, before `ResultEffect` consumes it.
- **Why the mailbox is application-scoped rather than per host:** mount this flow as a nested host
  and the launcher lives in the outer host. A host-scoped mailbox would break exactly this case.
- **`clear`** drops a value undelivered — for a flow that is abandoned rather than completed.

**Try it:** open *Review request*, start the review, continue twice, approve. The launcher shows the
decision, and the "pending when this screen came back" readout shows the name that was waiting.

## Doing this in your app

1. Declare `val X = resultKey<T>("feature.x")` next to the producing feature's routes.
2. Producer: `LocalNavigationResults.current.post(X, value)`, then pop — `popBack()` for one screen,
   `popBackTo { it is LauncherRoute }` for a flow.
3. Consumer: `ResultEffect(X) { value -> stateHolder.onX(value) }` in its composable.
4. Keep what the result updates in `rememberSaveable` or an entry-scoped ViewModel.
5. Treat "no result" as a first visit — results are in memory only and are lost on process death
   while the routes that would have consumed them are restored.

## Pitfalls

- **`remember` in the consumer** — the value is forgotten every time the producer covers it.
- **Reading `pending` while the producer is open** — the consumer is not composed then; nothing shows.
- **A result for a screen that does not exist yet** — that is an argument, not a result.
- **Posting twice before the consumer returns** — the second value replaces the first.
- **Relying on it after process death** — gone; use a route property or a repository for anything
  that must survive.

## Read more

- [Results](../../../../../../../../../../navigation/api/README.md#results) in the API guide
- [Passing data between screens](../../../../../../../../../../navigation/api/README.md#passing-data-between-screens) — every option side by side
- [All examples](../../../../../../../../../README.md#the-examples)
