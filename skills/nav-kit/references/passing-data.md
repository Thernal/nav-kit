# Passing data between screens

## Contents

1. Choose the mechanism
2. Recipe: return a value (results)
3. Recipe: share a value across a flow (arguments)
4. When it must survive process death

## 1. Choose the mechanism

| The value… | Use |
|---|---|
| is an id the next screen needs | a property on its route |
| goes back to a screen already on the stack (picker, confirmation, a flow's decision) | a **result** |
| is read or updated by several screens about to open (a draft, a selection carried through a wizard) | an **argument** |
| must survive process death | route ids + a repository, or a `SavedStateHandle` ViewModel on the entry that mounts the flow as a nested host |
| is large (bitmaps, documents, lists of models) | a repository; only its id travels |

Results and arguments are application-scoped, reached through composition locals, and **in memory only**.
A consumer that finds nothing treats it as a first visit or restarts its flow — never as an error.

## 2. Recipe: return a value

```kotlin
// Next to the PRODUCING feature's routes. Prefix the name with the feature.
val SelectedColour = resultKey<String>("palette.selected_colour")
```

Producer (the screen that closes):

```kotlin
val navigator = LocalNavigator.current
val results = LocalNavigationResults.current
Button(onClick = {
    results.post(key = SelectedColour, value = "Teal")
    navigator.popBack()                              // a flow: popBackTo { it is LauncherRoute }
}) { Text("Teal") }
```

Consumer (the screen underneath):

```kotlin
var colour by rememberSaveable { mutableStateOf<String?>(null) }
ResultEffect(SelectedColour) { picked -> colour = picked }
// or: ResultEffect(key = ReviewOutcome, onResult = viewModel::onDecision)
```

Packages: `io.thernal.navkit.navigation.api.presentation.result` (`resultKey`, `LocalNavigationResults`,
`ResultEffect`, `NavigationResults`).

Facts to rely on:

- `ResultEffect` consumes when the consumer is composed. A covered screen is not composed, so delivery
  happens when it is uncovered; a result posted while it is on top arrives immediately.
- Consuming removes the value: one delivery. A second `post` before consumption replaces the first.
- `clear(key)` drops a value undelivered (a flow abandoned). `pending: StateFlow<Set<String>>` exposes names
  only.
- The consumer's state must survive being covered: `rememberSaveable` or an entry-scoped ViewModel — never
  `remember`.
- Reading `pending` from the consumer while the producer is open shows nothing: the consumer is not composed.
- The same name declared with two different types throws when read
  (`` Result `name` was posted as … and read as … ``); a blank name throws when the key is created.
- Works across nested hosts (the mailbox is app-scoped).

## 3. Recipe: share a value across a flow

```kotlin
sealed interface CheckoutStepRoute : Route        // the flow's own routes
data object CheckoutAmount : CheckoutStepRoute
data object CheckoutAddress : CheckoutStepRoute
data object CheckoutSummary : CheckoutStepRoute

data class CheckoutDraft(val amount: String = "", val address: String = "")
val CheckoutDraftKey = argumentKey<CheckoutDraft>("checkout.draft")
private val inCheckout = whileInStack { it is CheckoutStepRoute }   // or whileRouteInStack<CheckoutStepRoute>()
```

Start the flow — put and push in the same action:

```kotlin
val navigator = LocalNavigator.current
val arguments = LocalNavigationArguments.current
Button(onClick = {
    arguments.put(key = CheckoutDraftKey, value = CheckoutDraft(), scope = inCheckout)
    navigator.push(CheckoutAmount)
}) { Text("Start checkout") }
```

Read and update on a step — the field owns its state and writes through:

```kotlin
val arguments = LocalNavigationArguments.current
var amount by rememberSaveable { mutableStateOf(arguments.get(CheckoutDraftKey)?.amount.orEmpty()) }

OutlinedTextField(
    value = amount,
    onValueChange = { text ->
        amount = text
        val current = arguments.get(CheckoutDraftKey) ?: CheckoutDraft()
        arguments.put(key = CheckoutDraftKey, value = current.copy(amount = text), scope = inCheckout)
    },
)
```

Leave the flow with any command that removes its routes (`popBackTo { it is CheckoutStart }`); do not
`remove` the argument by hand.

Packages: `io.thernal.navkit.navigation.api.presentation.argument` (`argumentKey`, `LocalNavigationArguments`,
`whileInStack`, `whileRouteInStack`, `ArgumentScope`, `NavigationArguments`).

How the lifetime works — and what breaks it:

- The **outermost** host calls `pruneFor(stack)` after each change of its stack.
- An argument whose scope has **never been alive** is kept until it is; it is dropped the first time it was
  alive and then is not. So put right before the push that starts the flow.
- A put whose scope **is already alive** (updating inside the flow) counts as alive, so it still dies with the
  flow.
- Scope to the flow's sealed type, not to one screen — then back, `popBackTo`, guard rewrites and deep links
  all end it correctly.
- An argument whose flow never starts is never pruned — `remove(key)` it.
- **Nested hosts:** only the outermost host prunes, against its own stack. Scope to a route of that stack (the
  route that mounts the nested flow); an argument scoped to routes existing only in a nested stack is never
  alive at the root and never pruned.
- **Not snapshot state.** A composable reading `get(key)` does not recompose when it changes; a text field
  bound to it loses input. Own the UI state, write through.
- Same name with two types throws on read; blank names throw. Never prune from a guard.

## 4. When it must survive process death

Results and arguments are lost on process death while the routes that needed them are restored (if the owner
persists the stack). For anything that must survive:

- put the id in the route and the data in a repository; or
- mount the flow as a nested host and keep the flow state in a ViewModel on the mounting entry, using
  `SavedStateHandle` (serializable, small values). Navigation3 clears that ViewModel when the entry is popped.

Test against process death, not rotation: an app-scoped store survives rotation and looks correct.
