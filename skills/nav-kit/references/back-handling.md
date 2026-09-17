# Back handling

## How back reaches the stack

Both the system back gesture/button (routed by the host) and an in-app Back button end in
`Navigator.popBack()`. `popBack()` asks the host's `BackDispatcher` first; a callback returning `true`
consumes back and nothing is popped. Callbacks run newest first.

Nothing else asks the dispatcher: `popBackTo`, `replace`, `replaceAll`, `navigate`, `buildStack`, deep links
and guard rewrites bypass it.

| Goal | Use |
|---|---|
| ask "discard changes?" when the user goes back from this screen | `NavigationBackHandler` |
| refuse every way out of this screen | a transition guard — see guards.md, recipe 4 |
| close an in-screen overlay (search mode, expanded panel) on back instead of leaving | `NavigationBackHandler(enabled = isOpen) { close() }` |

## Recipe: confirm on back

```kotlin
import io.thernal.navkit.navigation.impl.presentation.back.NavigationBackHandler   // navigation/impl

@Composable
fun DraftScreen() {
    val navigator = LocalNavigator.current
    var text by rememberSaveable { mutableStateOf("") }
    var isAsking by rememberSaveable { mutableStateOf(false) }

    NavigationBackHandler(enabled = text.isNotBlank()) { isAsking = true }

    // … content …

    if (isAsking) {
        AlertDialog(
            onDismissRequest = { isAsking = false },
            title = { Text("Discard the draft?") },
            confirmButton = {
                TextButton(onClick = {
                    isAsking = false
                    navigator.popBackTo(inclusive = true) { it is DraftRoute }   // skips the dispatcher
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { isAsking = false }) { Text("Keep editing") } },
        )
    }
}
```

- Leaving after confirmation with `popBack()` would be intercepted again while `enabled` is still true. Use
  `popBackTo`, or clear the condition first.
- Calling `navigator.popBack()` **inside** the handler callback does pop: a dispatch nested in another
  consumes nothing.
- `enabled` and `onBack` are read live; toggling does not re-register.
- The handler is active only while its screen is composed.

## Without depending on impl

```kotlin
@Composable
fun InterceptBack(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = LocalBackDispatcher.current            // api: presentation.back
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(dispatcher) {
        val registration = dispatcher.register(
            BackCallback {
                if (currentEnabled) {
                    currentOnBack()
                }
                currentEnabled                               // true consumes back
            },
        )
        onDispose { registration.close() }
    }
}
```

## Limits

- One `BackDispatcher` serves every host: a handler registered inside a nested host is also consulted by the
  outer host's `popBack()`.
- Bottom-sheet scenes: back walks the sheet's own entries before closing it.
- A handler cannot see the state a guard needs; if the rule must hold for jumps and links too, write the guard
  and keep the handler only for the dialog.
