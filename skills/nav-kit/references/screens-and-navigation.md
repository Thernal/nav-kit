# Screens, hosts and navigation

## Contents

1. Declaring routes
2. Registering screens
3. Navigating and reading outcomes
4. Navigating from a ViewModel
5. Nested hosts and tabs
6. Bottom sheets, scenes, decorators, transitions

## 1. Declaring routes

```kotlin
sealed interface CheckoutRoute : Route          // one sealed family per flow
data object CheckoutAmount : CheckoutRoute
data class CheckoutReceipt(val orderId: String) : CheckoutRoute
```

- Implement `io.thernal.navkit.navigation.api.presentation.model.Route`. Never import a Navigation3 key
  type in a feature.
- Ids only. A model, repository or platform object goes in a repository, with its id in the route.
- Seal per flow: exhaustive `when`, and one type for guards and argument scopes (`it is CheckoutRoute`).
- Routes other features navigate to live in the owning feature's public (`api`) module; internal steps
  stay internal.
- Marker interfaces opt a route into a guard: `data object Account : Route, AuthGuarded`.
- `TransientRoute` for placeholders that must not survive a restored stack.
- Keep routes distinct within one stack: equal routes share one saved state (Navigation3's default content
  key is `toString()`). To return to a route that may already be on the stack, `navigate(route)`.

## 2. Registering screens

In a feature, contribute a provider (the root calls every provider inside its host):

```kotlin
class CheckoutGraph(private val repository: CheckoutRepository) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<CheckoutAmount> { CheckoutAmountScreen(repository) }
        navEntry<CheckoutReceipt> { route -> ReceiptScreen(orderId = route.orderId) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface CheckoutBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideCheckoutGraph(repository: CheckoutRepository): NavigationGraphProvider {
            return CheckoutGraph(repository)
        }
    }
}
```

`NavigationGraphProvider { navEntry<X> { … } }` (lambda form) also works. Providers have receiver
`EntryProviderScope<Route>`, so they fit a host whose stack is typed `Route` (the root). A nested host over
its own type registers entries inline in its `NavigationHost { … }` block.

Registration rules (violations crash at runtime, not at compile time):

| Rule | Failure when broken |
|---|---|
| every route that can be on a host's stack has an entry in that host — include guard substitutes and deferral placeholders | `IllegalStateException: Unknown screen <route>` |
| each route class is registered once per host (two providers registering the same class count) | `IllegalArgumentException` "An `entry` with the same `clazz` has already been added" |
| a host's stack is never empty | `IllegalArgumentException: NavDisplay backstack cannot be empty` |

Entry content can take the route: `navEntry<Order> { route -> OrderScreen(route.id) }`. `viewModel { }`
inside an entry is scoped to that entry and cleared when it is popped.

## 3. Navigating and reading outcomes

```kotlin
val navigator = LocalNavigator.current   // io.thernal.navkit.navigation.api.presentation.navigator
```

| Command | Effect | Returns |
|---|---|---|
| `push(route)` | append | `NavigationOutcome` |
| `navigate(route, predicate = null)` | pop back to the last match (default: `== route`), or push if none | `NavigationOutcome` |
| `replace(route)` | replace the top | `NavigationOutcome` |
| `replaceAll(route)` / `replaceAll(routes)` | replace everything; `routes` non-empty | `NavigationOutcome` |
| `buildStack { add(…); removeAt(…) }` | arbitrary edit | `NavigationOutcome` |
| `popBack()` | back interceptors first, then pop if ≥ 2 routes | `Boolean` — consumed or moved |
| `popBack(count)` | `popBack()` × count | `Boolean` — any did |
| `popBackTo(inclusive = false) { predicate }` | jump back to the last match; never empties; skips interceptors | `Boolean` — moved |
| `canPop()` | — | more than one route |

Aliases: `pop()`, `pop(count)`, `popTo(…)`, `reset(route)`, `reset(routes)`.

- All commands, pops included, pass through guards before anything is written.
- Several commands in one handler compose (`popBack(); push(x)`): the host builds each on its newest write.
- `popBack(force = true)` can empty a one-route stack, which the host cannot render — only for an owner
  that then unmounts the host.

```kotlin
when (val outcome = navigator.push(CheckoutAmount)) {
    is NavigationOutcome.Applied -> Unit
    is NavigationOutcome.Rewritten -> showMessage(outcome.reason?.message)   // refused / redirected
    is NavigationOutcome.Deferred -> Unit                                    // a guard will decide; the host applies it
}
```

A command usually takes away the screen that issued it. If the outcome must be shown afterwards, record it
in an object that outlives the screen (an app-scoped log or the launching screen's ViewModel).

## 4. Navigating from a ViewModel

The ViewModel decides; the composable executes against `LocalNavigator`:

```kotlin
sealed interface ProfileNavigation {
    data class Edit(val profileId: String) : ProfileNavigation
    data object Close : ProfileNavigation
}

class ProfileViewModel(private val profileId: String) : ViewModel() {
    private val navigationEffects = Channel<ProfileNavigation>(Channel.BUFFERED)
    val navigation: Flow<ProfileNavigation> = navigationEffects.receiveAsFlow()

    fun onEditClicked() {
        navigationEffects.trySend(ProfileNavigation.Edit(profileId))
    }
}

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val navigator = LocalNavigator.current
    LaunchedEffect(viewModel, navigator) {
        viewModel.navigation.collect { effect ->
            when (effect) {
                is ProfileNavigation.Edit -> navigator.push(ProfileRoute.Edit(effect.profileId))
                ProfileNavigation.Close -> navigator.popBack()
            }
        }
    }
}
```

Never pass the navigator into the ViewModel or keep it in a singleton.

## 5. Nested hosts and tabs

Mount a `NavigationHost` inside an entry for tabs, a wizard, or any flow with its own stack. The stack's
owner is a ViewModel scoped to that entry.

```kotlin
@Composable
fun CheckoutFlowScreen() {
    val flow: CheckoutFlowViewModel = viewModel { CheckoutFlowViewModel() }
    val steps by flow.steps.collectAsState()
    val outer = LocalNavigator.current     // read BEFORE mounting; inside, LocalNavigator is the flow's

    NavigationHost(params = NavigationHostParams(backStack = steps, onBackStackChange = flow::onStepsChange)) {
        navEntry<CheckoutAmount> { AmountStep() }
        navEntry<CheckoutReceipt> { route -> ReceiptStep(route, onDone = { outer.popBack() }) }
        navEntry<SignInRoute> { route -> SignInScreen(route) }   // if an app-wide guard can substitute it here
    }
}
```

- `LocalNavigator` inside is the nested host's. Pass the outer navigator down to command the outer host.
- **Application-wide guards run on nested stacks too.** Register their substitutes in the nested host.
  Rules for this host only: `NavigationHostParams(guards = persistentListOf(…))`, run after the app-wide ones.
- Only the **outermost** host prunes arguments, against its own stack — scope a flow's arguments to the
  route that mounts the flow.
- Results work across hosts.
- System back goes to the innermost host that can pop; the kit's `BackDispatcher` is shared by all hosts.
- One level of nesting per flow.

Tabs, two shapes:

| | Tabs as one stack | A stack per tab |
|---|---|---|
| Owner holds | `ImmutableList<Route>` | `Map<Tab, ImmutableList<Route>>` + selected tab |
| Select | `stack = persistentListOf(tabRoot)` | select; host gets `stacks.getValue(tab)` |
| `onBackStackChange` | `stack = next` | `stacks += selected to next` |
| Depth kept across switches | no | yes (routes only) |
| Re-select active tab | — | reset to its root |

With a stack per tab over one host, a hidden tab's entries count as popped: their `rememberSaveable` and
entry ViewModels are cleared. Keep state that must survive a switch in the tab-owning ViewModel.

## 6. Bottom sheets, scenes, decorators, transitions

```kotlin
bottomSheetEntry<CouponSheet> { CouponSheetContent() }
```

Consecutive sheet entries on top form one sheet surface; back walks its steps, then closes it. The scene is
bare — add scrim, handle and dismiss from the design system, or supply a `SceneStrategy` via
`NavigationHostParams.sceneStrategies` (consulted before the built-in sheet and single-pane strategies).

`NavigationHostParams.decorators` are appended after the host's saveable-state and ViewModel-store
decorators.

Transitions (`null` = default):

```kotlin
// io.thernal.navkit.navigation.impl.presentation.host.NavAnimations
val up = remember { NavAnimations.slideTo<Route>(direction = { SlideDirection.UP }) }
val down = remember { NavAnimations.slideTo<Route>(direction = { SlideDirection.DOWN }) }
NavigationHostParams(backStack = steps, onBackStackChange = flow::onStepsChange, transitionSpec = up, popTransitionSpec = down)

val none: NavTransitionScope<Route> = { ContentTransform(EnterTransition.None, ExitTransition.None) }
```

`NavAnimations.push/pop/predictivePop(duration, easing)` are the defaults; `NavigationDefaults` holds the
numbers (260 ms, fade 180 ms, `FastOutSlowInEasing`). Overlay scenes (sheets) are not animated by the defaults.
