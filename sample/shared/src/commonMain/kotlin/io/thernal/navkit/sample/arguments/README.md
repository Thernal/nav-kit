# Arguments — a value travelling forwards

Catalog group **Arguments**: [One value, two screens](#simple-one-value-two-screens) (simple) and
[Checkout draft](#advanced-checkout-draft) (advanced).

A screen that opens others sets a value; screens that do not exist yet read it — without it being
threaded through every route in between — and it disappears once those screens are gone. The store
is `NavigationArguments`; what makes it work is that **an argument's lifetime is a question about the
back stack**, not a count of who is using it.

| File | What is in it |
|---|---|
| [`ArgumentsRoutes.kt`](ArgumentsRoutes.kt) | the routes, the sealed `CheckoutStepRoute` the draft is scoped to, `CheckoutDraft`, the two keys |
| [`GreetingScreens.kt`](GreetingScreens.kt) | put, push, read |
| [`CheckoutScreens.kt`](CheckoutScreens.kt) | one draft read and updated on four screens, dropped when the flow leaves |
| [`ArgumentsBindings.kt`](ArgumentsBindings.kt) | screens and catalog entries |

## Which mechanism?

| The value… | Use |
|---|---|
| is an id the next screen needs | a property on its route — see [`basics`](../basics/README.md) |
| goes back to a screen already on the stack | a result — see [`results`](../results/README.md) |
| is read (and maybe updated) by several screens about to open | **an argument — this package** |
| must survive process death | a route id plus a repository, or a ViewModel with `SavedStateHandle` on the entry that mounts the flow as a nested host |

## Why not a reference count

The obvious design — each consumer registers on the value and releases it on `DisposableEffect`,
dropping it at zero — fails on its trigger. Navigation3 composes only the entries of the current
scene, so a screen that pushes the next one leaves composition while it is still in the stack; the
count reaches zero one push early and the last screen finds nothing. The back stack already states
who is present, so the stack decides.

## Simple: One value, two screens

```kotlin
val GreetingName = argumentKey<String>("arguments.greeting_name")
```

Put and push in the same action:

```kotlin
BottomAction(
    label = "Send the card",
    onClick = {
        arguments.put(
            key = GreetingName,
            value = name,
            scope = whileInStack { it is GreetingReaderRoute },
        )
        navigator.push(GreetingReaderRoute)
    },
)
```

Read it anywhere the scope is alive, without it being in the route:

```kotlin
@Composable
fun GreetingReaderScreen() {
    val name = LocalNavigationArguments.current.get(GreetingName)
    HeroCard(title = if (name == null) "This card is empty" else "Hello, $name!", …)
}
```

What to notice:

- **The ordering rule.** An argument whose scope has never been alive is kept until it is. Putting it
  right before the push means the stack change that starts the flow is the first to see it alive —
  and cannot delete it.
- **Going back drops it.** After the pop no route matching the scope is on the stack, and the host
  prunes the store after every stack change.

**Try it:** open *One value, two screens*, change the name, *Send the card*. Go back and send it
again: the composer puts the value again before each push.

## Advanced: Checkout draft

One draft, read and updated on four consecutive screens, gone the moment the flow leaves the stack.

```kotlin
sealed interface CheckoutStepRoute : ArgumentsRoute
data object CheckoutAmountRoute : CheckoutStepRoute
data object CheckoutAddressRoute : CheckoutStepRoute
data object CheckoutPaymentRoute : CheckoutStepRoute
data object CheckoutSummaryRoute : CheckoutStepRoute

data class CheckoutDraft(val amount: String = "", val address: String = "", val method: String = "")
val CheckoutDraftKey = argumentKey<CheckoutDraft>("arguments.checkout_draft")

/** Alive while any step of the flow is on the stack — the start screen is not a step. */
private val inCheckout: ArgumentScope = whileInStack { route -> route is CheckoutStepRoute }
```

The start screen seeds the draft and enters the flow; the summary leaves it:

```kotlin
arguments.put(key = CheckoutDraftKey, value = CheckoutDraft(), scope = inCheckout)
navigator.push(CheckoutAmountRoute)

// on the summary
navigator.popBackTo { candidate -> candidate is CheckoutStartRoute }
```

Each step owns its text field and writes every change through:

```kotlin
@Composable
private fun CheckoutStep(
    title: String,
    label: String,
    read: (CheckoutDraft) -> String,
    write: (CheckoutDraft, String) -> CheckoutDraft,
    next: CheckoutStepRoute,
) {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    // Read once, when the step is first shown; saved with the entry after that.
    var entered by rememberSaveable { mutableStateOf(read(arguments.draft())) }

    OutlinedTextField(
        value = entered,
        onValueChange = { text ->
            entered = text
            arguments.update { draft -> write(draft, text) }
        },
        label = { Text(text = label) },
    )
    BottomAction(label = "Continue", onClick = { navigator.push(next) })
}

private fun NavigationArguments.update(change: (CheckoutDraft) -> CheckoutDraft) {
    // Applied to the draft as it is now, so two steps never write over each other's fields.
    put(key = CheckoutDraftKey, value = change(draft()), scope = inCheckout)
}
```

What to notice:

- **Scope to the flow's sealed type, not to a screen.** `it is CheckoutStepRoute` stays alive across
  all four steps, and back, `popBackTo`, a guard rewrite or a deep link that removes the steps all end
  the draft without being handled separately.
- **Updating inside the flow keeps the flow's lifetime.** A put whose scope is already alive counts as
  alive from the start, so the draft re-put on every keystroke still dies when the flow is left.
- **The store is not snapshot state.** A field bound straight to `arguments.get(…)` never recomposes:
  every keystroke was reverted and the revert written back as an empty string. The field keeps its own
  saveable state and writes through.
- **Nothing cleans up by hand.** Leaving pops every step at once; the next prune finds none of them
  alive and drops the draft.

**Try it:** open *Checkout draft*, *Buy a gift card*, fill the amount and the recipient, pick a
payment method, and read them on the summary. *Confirm*: the start screen's *Draft outside the flow*
readout says the draft is `null`.

## Doing this in your app

1. Give the flow a sealed route type; declare `argumentKey<T>("feature.name")` next to it.
2. In the action that starts the flow: `put(key, value, whileInStack { it is FlowRoute })`, then push
   the first step.
3. Read with `get(key)` on any step; treat `null` as "restart the flow" — arguments are in memory
   only and are lost on process death while the routes are restored.
4. For editable fields, keep the field in `rememberSaveable` and write each change through, applied to
   the current value.
5. Leave with `popBackTo` (or any command that removes the steps); do not remove the argument by hand.

## Pitfalls

- **Scoping to one screen** — the value dies whenever that one route leaves the stack — a flow that
  advances with `replace`, a guard that drops it — while later steps still read it.
- **Putting the argument long before the push** — kept until its scope is first alive; if the flow
  never starts, it is never pruned. `remove` it.
- **Binding a text field straight to the store** — typing is lost.
- **Scoping to routes of a nested host** — only the outermost host prunes, against its own stack; such
  an argument is never alive there and never pruned. Scope to the route that mounts the nested host.
- **Pruning from a guard** — never; `evaluate` must stay pure.

## Read more

- [Arguments](../../../../../../../../../../navigation/api/README.md#arguments) in the API guide
- [What survives what](../../../../../../../../../../navigation/api/README.md#what-survives-what)
- [All examples](../../../../../../../../../README.md#the-examples)
