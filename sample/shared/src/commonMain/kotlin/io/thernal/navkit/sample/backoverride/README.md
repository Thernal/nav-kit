# Back handling — two ways to refuse leaving

Catalog group **Back handling**: [Confirm before leaving](#simple-confirm-before-leaving) (simple)
and [Unsaved work](#advanced-unsaved-work) (advanced).

"Discard your changes?" has two very different shapes, and choosing the wrong one is the usual bug:

| You want to… | Use | Hears |
|---|---|---|
| ask before **back** on one screen | `NavigationBackHandler` | the system gesture and every `popBack()` — only while the screen is composed |
| refuse **every way out** of a screen | a transition `NavigationGuard` | every stack change: back, `popBackTo`, `replaceAll`, `navigate`, deep links, guard rewrites |

| File | What is in it |
|---|---|
| [`BackRoutes.kt`](BackRoutes.kt) | the routes and `UnsavedWork`, the application's own `BlockReason` |
| [`DraftEditorScreen.kt`](DraftEditorScreen.kt) | a screen that intercepts back and asks |
| [`ArticleDraftStore.kt`](ArticleDraftStore.kt) | editor state held outside composition, where a guard can read it |
| [`UnsavedWorkGuard.kt`](UnsavedWorkGuard.kt) | a guard about leaving a screen |
| [`ArticleScreens.kt`](ArticleScreens.kt) | the editor, and commands that try to leave it |
| [`BackBindings.kt`](BackBindings.kt) | contributes the guard into the kit's guard set |

## Simple: Confirm before leaving

```kotlin
@Composable
fun DraftEditorScreen() {
    val navigator = LocalNavigator.current
    var text by rememberSaveable { mutableStateOf("") }
    var isAsking by rememberSaveable { mutableStateOf(false) }

    NavigationBackHandler(enabled = text.isNotBlank()) {
        isAsking = true
    }

    // … the text field …

    if (!isAsking) return
    AlertDialog(
        onDismissRequest = { isAsking = false },
        title = { Text(text = "Discard the draft?") },
        confirmButton = {
            TextButton(
                onClick = {
                    isAsking = false
                    navigator.popBackTo(inclusive = true) { route -> route is DraftEditorRoute }
                },
            ) { Text(text = "Discard") }
        },
        dismissButton = { TextButton(onClick = { isAsking = false }) { Text(text = "Keep editing") } },
    )
}
```

What to notice:

- **One back path.** The handler registers with the `BackDispatcher` the mounted host provides, and
  `Navigator.popBack()` consults that dispatcher first — whether back came from the system gesture or
  from the scaffold's Back button. Only the gesture used to ask, which made "discard?" work in one of
  the two and silently not in the other.
- **Discarding uses `popBackTo`**, which does not consult the dispatcher: it is a jump, not a back.
  Calling `popBack()` there would be intercepted by this same handler again, because the text is still
  not blank.
- **`enabled` is read live.** Toggling it re-registers nothing, so the callback keeps its place among
  other interceptors.
- `NavigationBackHandler` lives in `navigation/impl`. A module that must not see `impl` registers
  through `LocalBackDispatcher.current.register(BackCallback { … })` in a `DisposableEffect` instead.

**Try it:** open *Confirm before leaving*, type a note, then press back with the gesture — and then
with the arrow in the top bar. Both ask. Clear the text and back leaves without asking.

**The limit.** A handler fires only while its screen is composed and only through `popBack()`. It
cannot stop a `popBackTo` or `replaceAll` issued elsewhere, a deep link, or a guard rewrite — a jump
that skips the screen. That is the next example.

## Advanced: Unsaved work

"The editor may not be left with unsaved work" is a statement about the difference between the stack
that was and the stack being proposed — a rule about a **transition**. A destination-only rule cannot
express it, so the guard implements `NavigationGuard` directly and compares `old` with `new`:

```kotlin
class UnsavedWorkGuard(private val drafts: ArticleDraftStore) : NavigationGuard {
    private val reason: BlockReason = UnsavedWork

    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        val wasEditing = old.any { route -> route is ArticleEditorRoute }
        val isStillEditing = new.any { route -> route is ArticleEditorRoute }
        if (wasEditing && !isStillEditing && drafts.hasUnsavedChanges) {
            // Returning the previous stack *is* the refusal.
            return GuardVerdict.Resolved(stack = old, reason = reason)
        }
        return GuardVerdict.Resolved(new)
    }
}

data object UnsavedWork : BlockReason {
    override val message: String = "The article has unsaved changes"
}
```

It is contributed into the kit's guard set, so it applies to every host:

```kotlin
@Provides
@IntoSet
fun provideUnsavedWorkGuard(drafts: ArticleDraftStore): NavigationGuard {
    return UnsavedWorkGuard(drafts)
}
```

The editor tries to leave in ways a back handler would never see, and reports what happened:

```kotlin
val didMove = navigator.popBackTo { route -> route is ArticleHomeRoute }   // false while unsaved

when (val outcome = navigator.replaceAll(ArticleHomeRoute)) {
    is NavigationOutcome.Applied -> "left the editor"
    is NavigationOutcome.Rewritten -> "refused: ${outcome.reason?.message ?: "no reason"}"
    is NavigationOutcome.Deferred -> "a guard needs time"
}
```

What to notice:

- **The guard reads state outside composition.** Guards run before a stack reaches the host, never
  during composition, so the editor's text lives in [`ArticleDraftStore`](ArticleDraftStore.kt), an
  application-scoped object, not in `remember`.
- **That store is snapshot state, not a `StateFlow`.** The text field is bound to it; a flow collected
  into composition hands the field its value a frame late, which drops characters under fast typing.
  Snapshot state is synchronous, and `hasUnsavedChanges` is observable for free.
- **The refusal carries the application's own `BlockReason`.** It comes back on
  `NavigationOutcome.Rewritten.reason`; the kit never decides whether it reads as a prompt or an error.
- **The rule is phrased about the editor's own route.** `old` does not advance while the runner folds
  guards, so a rule broad enough to reject *any* difference between the stacks would undo every other
  guard's rewrite, and the runner would fail at its round limit instead of settling.

**Try it:** open *Unsaved work*, open the draft, type. Try *Jump home · popBackTo*, *Reset the stack ·
replaceAll*, the back arrow and the gesture — all refused, and the *Last attempt to leave* readout says
why. *Save* or *Discard*, and every way out works again.

## Doing this in your app

- **Confirm on back for one screen:** `NavigationBackHandler(enabled = hasChanges) { showDialog() }`;
  leave after confirming with `popBackTo(inclusive = true) { it is ThisRoute }`, or disable the
  handler first and then `popBack()`.
- **Refuse every way out:** keep the dirty state in an application-scoped object; write a
  `NavigationGuard` that returns `Resolved(old, reason)` when this screen's route leaves the stack
  while dirty; contribute it `@IntoSet`; show `outcome.reason` where a command is refused.
- Both together is fine: the handler asks on back, the guard backstops every other exit.

## Pitfalls

- **Expecting a back handler to stop `popBackTo`, `replaceAll` or a deep link** — it cannot; use a guard.
- **Leaving with `popBack()` from the confirm button while the handler is still enabled** — intercepted
  again; use `popBackTo` or disable first. (Calling `popBack()` *inside* the handler callback does pop —
  a nested dispatch consumes nothing.)
- **A guard reading `remember`ed screen state** — it cannot see it.
- **A transition guard that refuses any change between `old` and `new`** — fights every rewriting
  guard until `Navigation guards did not settle after 8 rounds`.

## Read more

- [Back handling](../../../../../../../../../../navigation/api/README.md#back-handling) and [Transition rules](../../../../../../../../../../navigation/api/README.md#transition-rules) in the API guide
- [Guards](../guards/README.md) — destination rules and deferred decisions
- [All examples](../../../../../../../../../README.md#the-examples)
