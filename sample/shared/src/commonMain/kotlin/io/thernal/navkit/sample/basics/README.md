# Navigation — commands and what came of them

Catalog group **Navigation**: [Push and pop](#simple-push-and-pop) (simple) and
[Order flow](#advanced-order-flow) (advanced).

Moving between screens without any screen owning the back stack, and knowing at the call site
whether a command actually happened. Every other example in the sample is built from these
commands.

| File | What is in it |
|---|---|
| [`BasicsRoutes.kt`](BasicsRoutes.kt) | the sealed `BasicsRoute` family — five routes, one of them carrying an id |
| [`BasicsSimpleScreens.kt`](BasicsSimpleScreens.kt) | push and pop |
| [`WizardScreens.kt`](WizardScreens.kt) | `navigate` with a predicate, `popBackTo`, `replaceAll`, and reading `NavigationOutcome` |
| [`OrderFlowLog.kt`](OrderFlowLog.kt) | where an outcome is kept once the screen that asked is gone |
| [`BasicsBindings.kt`](BasicsBindings.kt) | how a feature contributes its screens and its catalog entries — the pattern every example follows |

## Simple: Push and pop

A route is a small immutable value. A screen that needs one opens it with the navigator of the host
it is in:

```kotlin
sealed interface BasicsRoute : SampleRoute
data object BasicsHomeRoute : BasicsRoute
data class BasicsDetailRoute(val id: String) : BasicsRoute
```

```kotlin
@Composable
fun BasicsHomeScreen() {
    val navigator = LocalNavigator.current
    ListRow(title = "Ceramic mug", onClick = { navigator.push(BasicsDetailRoute(id = "A")) })
}

@Composable
fun BasicsDetailScreen(route: BasicsDetailRoute) {
    // The route carries the id — no shared state, no store.
}
```

The back arrow in [`SampleScreen`](../ui/SampleScreen.kt)'s top bar calls `navigator.popBack()`, shown
only while `navigator.canPop()` is true.

What to notice:

- **Nothing is injected.** `LocalNavigator` is provided by the mounted host, so a screen reaches it
  without a constructor parameter and without knowing which host it is in. The same screen mounted
  inside a nested host drives that host instead.
- **The host does not change the stack itself.** `push` computes the next stack, runs it past the
  guards, and hands it to the owner — here the root `RootViewModel` — through `onBackStackChange`.
  The host renders whatever the owner hands back.
- **The route is the argument.** An id travels in the route; anything larger belongs in a repository,
  with only its id here.

**Try it:** open *Push and pop*, open the mug, go back with the arrow in the top bar, then with the
system gesture. Both pops are the same `popBack()`.

## Advanced: Order flow

A flow needs more than push and pop, and each command can be refused, redirected or put on hold by
a guard. So every command that can add routes answers with a `NavigationOutcome`:

```kotlin
private fun NavigationOutcome.describe(): String {
    return when (this) {
        is NavigationOutcome.Applied -> "applied · ${stack.size} route(s)"
        is NavigationOutcome.Rewritten -> "rewritten by a guard · ${reason?.message ?: "no reason given"}"
        is NavigationOutcome.Deferred -> "deferred · a guard needs time"
    }
}
```

The flow is `WizardRoute → WizardStepRoute(1) → (2) → (3)`, and every step offers the commands a real
flow reaches for:

| Button | Command | Why this command |
|---|---|---|
| Continue to … | `push(WizardStepRoute(step + 1))` | plain forward move |
| Edit delivery · back to step 1 | `navigate(WizardStepRoute(1), predicate = { it is WizardStepRoute && it.step == 1 })` | returns to the step already in the stack instead of pushing a second one — one stack write, so guards see the real destination |
| Cancel checkout | `popBackTo { it is WizardRoute }` | a jump: answers `Boolean` — did the stack move — and skips back interceptors |
| Place order | `replaceAll(listOf(CatalogRoute, WizardDoneRoute))` | a whole new stack in one command, guarded on every route it proposes |

```kotlin
SecondaryButton(
    label = "Edit delivery · back to step 1",
    onClick = {
        val outcome = navigator.navigate(
            route = WizardStepRoute(step = 1),
            predicate = { candidate -> candidate is WizardStepRoute && candidate.step == 1 },
        )
        log.record(command = "navigate to step 1", outcome = outcome.describe())
    },
)
```

**Where the answer goes.** A command usually takes away the screen that issued it — pushed over,
popped, replaced. An outcome kept in that screen's state is thrown away with it, so the flow records
outcomes in [`OrderFlowLog`](OrderFlowLog.kt), an application-scoped object every step reads:

```kotlin
class OrderFlowLog {
    private val mutableLast = MutableStateFlow("—")
    val last: StateFlow<String> = mutableLast.asStateFlow()
    fun record(command: String, outcome: String) { mutableLast.value = "$command → $outcome" }
}
```

On the done screen, `canPop()` answers from the stack the host is working from — after guards —
not from what the caller last proposed.

**Try it:** open *Order flow*, tap *Checkout*, walk to step 3, use *Edit delivery · back to step 1*
and watch the *Last outcome* readout; walk forward again and *Place order*: the stack becomes
catalog → done, and back goes to the catalog.

## How a feature registers itself

[`BasicsBindings.kt`](BasicsBindings.kt) is the template every example in the sample follows. The
composition root imports none of it.

```kotlin
private class BasicsGraph(private val log: OrderFlowLog) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<BasicsHomeRoute> { BasicsHomeScreen() }
        navEntry<BasicsDetailRoute> { route -> BasicsDetailScreen(route) }
        navEntry<WizardRoute> { WizardScreen(log) }
        navEntry<WizardStepRoute> { route -> WizardStepScreen(route = route, log = log) }
        navEntry<WizardDoneRoute> { WizardDoneScreen(log) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface BasicsBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideOrderFlowLog(): OrderFlowLog { return OrderFlowLog() }

        @Provides
        @IntoSet
        fun provideBasicsGraph(log: OrderFlowLog): NavigationGraphProvider { return BasicsGraph(log) }

        // The catalog card. The index screen imports no example either.
        @Provides
        @IntoSet
        fun provideBasicsSimpleExample(): SampleExample {
            return SampleExample(
                topic = Topic.NAVIGATION,
                kind = ExampleKind.SIMPLE,
                title = "Push and pop",
                summary = "One screen opens another and the other comes back.",
                route = BasicsHomeRoute,
            )
        }
    }
}
```

## Doing this in your app

1. Declare a sealed route family per feature or flow; ids in the routes, nothing heavier.
2. Register every route in a `NavigationGraphProvider` and contribute it `@IntoSet`.
3. In screens, read `LocalNavigator.current` and call commands from event handlers — or emit a
   navigation effect from the ViewModel and replay it against `LocalNavigator.current`.
4. Use `navigate(route, predicate)` when the destination may already be in the stack, `popBackTo`
   to jump, `replaceAll` to start over.
5. Read the `NavigationOutcome` wherever a guard could change the answer, and keep what you do with it
   somewhere that outlives the screen.

## Pitfalls

- **An outcome stored in the screen that issued the command** is lost with that screen. This example
  showed "—" forever until the log moved out of the step.
- **Pushing a route that is already in the stack** gives two entries sharing one saved state; use
  `navigate`.
- **`replaceAll(emptyList())`** fails, and a host can never render an empty stack.
- **`popBackTo` returning `true` is not "a match was found"** — it is "the stack moved". A refused jump
  or a match already on top answers `false`.
- **Two commands in one click handler compose**: the host builds each command on its newest write,
  even though the owner hands the stack back a frame later.

## Read more

- [Navigator](../../../../../../../../../../navigation/api/README.md#navigator) — every command and what it returns
- [Outcomes](../../../../../../../../../../navigation/api/README.md#outcomes) and [calling from a ViewModel](../../../../../../../../../../navigation/api/README.md#calling-from-a-viewmodel)
- [Feature-owned graphs](../../../../../../../../../../navigation/api/README.md#feature-owned-graphs)
- [All examples](../../../../../../../../../README.md#the-examples)
