# navigation/api

The public surface of nav-kit — everything an application or a feature module compiles against:
routes, the host that renders a back stack, the navigator that changes it, guards that decide which
stacks may exist, results and arguments that carry data between screens, back interception, deep
links, transitions and the navigation event stream.

This module holds contracts only. `navigation/impl` implements them and `navigation/wiring`
installs them into an application graph, so a feature module depends on `api` and nothing else.

- **Runnable examples** of every section: [`sample/`](../../sample/README.md) — one simple and one
  advanced example per capability, on Android and iOS.
- **Why** each contract has the shape it has: [`navigation/README.md`](../README.md).
- **Agents** get the same material as a skill: [`skills/nav-kit`](../../skills/nav-kit/SKILL.md).

Targets: `android`, `iosArm64`, `iosSimulatorArm64`. Built on Navigation3 — the multiplatform
`navigation3-runtime` and JetBrains' `navigation3-ui` port; versions in
[`gradle/libs.versions.toml`](../../gradle/libs.versions.toml).

## Contents

- [Which tool do I need](#which-tool-do-i-need)
- [The model](#the-model)
- [Installing](#installing)
- [Routes](#routes)
- [Mounting a host](#mounting-a-host)
- [Navigator](#navigator)
- [Guards](#guards)
- [Passing data between screens](#passing-data-between-screens)
- [Back handling](#back-handling)
- [Deep links](#deep-links)
- [Nested hosts and tabs](#nested-hosts-and-tabs)
- [Bottom sheets, scenes and transitions](#bottom-sheets-scenes-and-transitions)
- [Navigation events](#navigation-events)
- [Composition locals](#composition-locals)
- [What survives what](#what-survives-what)
- [Testing](#testing)
- [Rules checklist](#rules-checklist)
- [Known limitations](#known-limitations)
- [API index](#api-index)

## Which tool do I need

Start from what you are trying to do; the section named is where the mechanism is explained.

| I want to… | Use | Section | Sample |
|---|---|---|---|
| declare a destination | a `data object`/`data class` implementing `Route` | [Routes](#routes) | [basics](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) |
| show a back stack on screen | `NavigationHost` + `NavigationHostParams` | [Mounting a host](#mounting-a-host) | [sample](../../sample/README.md#set-nav-kit-up-in-your-own-app) |
| let a feature register its own screens | `NavigationGraphProvider` | [Feature-owned graphs](#feature-owned-graphs) | [basics](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) |
| open, close or replace screens | `LocalNavigator.current` | [Navigator](#navigator) | [basics](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) |
| know whether a command really happened | the returned `NavigationOutcome` | [Outcomes](#outcomes) | [basics](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) |
| pass an id to the next screen | a property on the route | [Routes](#routes) | [basics](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) |
| return a value to the screen underneath | `resultKey` + `post` + `ResultEffect` | [Results](#results) | [results](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/results/README.md) |
| share one value across the next several screens | `argumentKey` + `put(…, whileInStack { … })` | [Arguments](#arguments) | [arguments](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/arguments/README.md) |
| keep users out of a screen unless a condition holds | `RouteGuard` over a marker interface | [Destination rules](#destination-rules-with-routeguard) | [guards](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/README.md) |
| send users to sign-in and continue afterwards | `RouteGuard` substituting `SignIn(next = route)` | [Destination rules](#destination-rules-with-routeguard) | [guards](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/README.md) |
| remove a screen when its condition stops holding | `NavigationGuard.invalidations` | [Staying correct](#staying-correct-when-the-answer-changes) | [guards](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/README.md) |
| decide asynchronously — token refresh, PIN, server check | `GuardVerdict.Deferred` + a `TransientRoute` placeholder | [Deciding later](#deciding-later) | [guards](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/README.md) |
| ask "discard changes?" when back is pressed on one screen | `NavigationBackHandler` | [Back handling](#back-handling) | [backoverride](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/backoverride/README.md) |
| refuse every way out of a screen, not only back | a transition `NavigationGuard` | [Transition rules](#transition-rules) | [backoverride](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/backoverride/README.md) |
| open the app from a link or a notification | `DeepLinkHandler` + `DeepLinkIngress` | [Deep links](#deep-links) | [deeplinks](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/deeplinks/README.md) |
| accept links on a scheme or a domain | a `DeepLinkBase` contributed `@IntoSet` | [Link bases](#link-bases) | [deeplinks](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/deeplinks/README.md) |
| build a link to share | `buildDeepLinkUri(base, page)` / `DeepLinkPage.buildUri(base)` | [Outbound links](#outbound-links) | — |
| tabs, or a wizard with a back stack of its own | a nested `NavigationHost` | [Nested hosts and tabs](#nested-hosts-and-tabs) | [tabs](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/tabs/README.md) |
| show a route as a bottom sheet | `bottomSheetEntry` | [Bottom sheets](#bottom-sheets-scenes-and-transitions) | [sheets](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/sheets/README.md) |
| show something instead of crashing on a route a host does not register | `NavigationHostParams.fallback` | [Fallback entries](#fallback-entries) | [sample](../../sample/README.md) |
| change or turn off the animations | `transitionSpec` and friends on `NavigationHostParams` | [Transitions](#transitions) | — |
| log, measure or test what navigation did | `NavigationEventSink` | [Navigation events](#navigation-events) | [`SampleBindings.kt`](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleBindings.kt) |

## The model

```
            ┌────────── root state holder (a ViewModel) ──────────┐
 deep link ─▶ backStack: StateFlow<ImmutableList<Route>>            │
            │ onBackStackChange(next) { backStack.value = next }   │
            └───────────────┬───────────────────────▲──────────────┘
                   backStack│                       │every write: commands,
                            ▼                       │guard corrections, settled deferrals
                     NavigationHost ── guards ──▶ NavDisplay
                            │ provides LocalNavigator, LocalBackDispatcher
                            ▼
                         screens ── navigator.push(route) ──▶ NavigationOutcome
```

Five facts carry the rest of this document:

1. **A route is a value.** `data object Home : Route`. It names a destination and carries small
   identifiers, nothing else.
2. **A host never owns its stack.** It is controlled like a `TextField`: the caller hands it
   `backStack` and receives every change through `onBackStackChange`. The caller — normally a
   ViewModel — is the single place the navigation state lives.
3. **Screens command the host they are in** through `LocalNavigator.current`. The navigator holds
   no state, is built per host, and is never injected.
4. **Guards decide which stacks may exist.** Every command, every stack handed to a host directly
   (a deep link, a restored stack) and every guard invalidation goes through them before anything
   renders. That is why commands return a `NavigationOutcome` rather than `Unit`.
5. **Application-wide services arrive as composition locals** installed once at the root: the host
   renderer, the results mailbox and the argument store.

## Installing

### Modules

| Module | Who depends on it | What it holds |
|---|---|---|
| `:navigation:api` | every feature module, and the app | the contracts in this document; re-exports Navigation3, coroutines and immutable collections |
| `:navigation:impl` | the module that builds the graph; any module using `NavigationBackHandler` or `NavAnimations` | the Navigation3 host, the navigator, guards runner, stores, deep-link parsing |
| `:navigation:wiring` | the module that declares the [Metro](https://github.com/ZacSweers/metro) graph | `NavigationWiring`, the bindings below |

The modules are not published to a Maven repository; build against them from source.
[`sample/shared/build.gradle.kts`](../../sample/shared/build.gradle.kts) is the reference consumer:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.navigation.api)
            implementation(projects.navigation.impl)
            implementation(projects.navigation.wiring)   // leave out when wiring by hand
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.lifecycle.viewmodel.compose) // the root stack lives in a ViewModel
        }
    }
}
```

### With Metro

`NavigationWiring` is a `@BindingContainer` contributed to `AppScope`, so a graph over `AppScope`
picks it up once the module is on the classpath. It binds:

| Binding | Scope | Built from |
|---|---|---|
| `NavigationHostRenderer` | single | the guard runner, back dispatcher, argument pruner and event sink below |
| `NavigationGuardRunner` | single | `Set<NavigationGuard>` — contribute guards `@IntoSet` |
| `DeepLinkDispatcher` | single | `Set<DeepLinkHandler>` and `Set<DeepLinkBase>` — contribute both `@IntoSet` |
| `NavigationEventSink` | single | `Set<NavigationEventSink>` fanned out; `NoOp` when empty |
| `DeepLinkIngress` + `DeepLinkEvents` | single, **one object** | `RuntimeDeepLinkBridge` |
| `NavigationArguments` + `ArgumentPruner` | single, **one object** | `NavigationArgumentsImpl` |
| `NavigationResults` | single | `NavigationResultsImpl` |
| `BackDispatcher` | single | `BackDispatcherImpl` |
| `Set<ProvidedValue<*>>` | — | `LocalNavigationHostRenderer`, `LocalNavigationResults`, `LocalNavigationArguments` |

`Navigator` is deliberately not bound: a host builds its own.

The application declares its graph, its own multibinding for screens, and the bases its links start
with:

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val providedValues: Set<ProvidedValue<*>>
    val graphProviders: Set<NavigationGraphProvider>
    val deepLinkEvents: DeepLinkEvents
    val deepLinkDispatcher: DeepLinkDispatcher
    val deepLinkIngress: DeepLinkIngress
}

@BindingContainer
@ContributesTo(AppScope::class)
interface AppBindings {
    @Multibinds(allowEmpty = true)
    val graphProviders: Set<NavigationGraphProvider>

    companion object {
        // The scheme and the origin AndroidManifest.xml and Info.plist declare — see "Link bases".
        @Provides
        @IntoSet
        fun provideAppSchemeBase(): DeepLinkBase {
            return DeepLinkBase("navkit://")
        }

        @Provides
        @IntoSet
        fun provideWebOriginBase(): DeepLinkBase {
            return DeepLinkBase("https://example.com")
        }
    }
}
```

Create the graph **once per process** — in the Android `Application`, in a process-wide `lazy` on
iOS — and hand it to the composition. A graph remembered by the composition is rebuilt when an
Android activity is recreated, and takes the session, the results and the arguments with it while
the root back stack, held by a ViewModel, survives and points at state that no longer exists.

### Without a DI framework

`api` and `impl` name no container. The same object graph, by hand — one instance per process:

```kotlin
class Navigation(
    guards: List<NavigationGuard>,
    deepLinkHandlers: Set<DeepLinkHandler>,
    deepLinkBases: Set<DeepLinkBase>,
    sinks: List<NavigationEventSink> = emptyList(),
) {
    private val bridge = RuntimeDeepLinkBridge()
    private val arguments = NavigationArgumentsImpl()
    private val results: NavigationResults = NavigationResultsImpl()

    val deepLinkIngress: DeepLinkIngress = bridge
    val deepLinkEvents: DeepLinkEvents = bridge
    val deepLinkDispatcher: DeepLinkDispatcher = DeepLinkDispatcherImpl(
        handlers = deepLinkHandlers,
        bases = deepLinkBases,
    )

    private val renderer: NavigationHostRenderer = NavigationHostRendererImpl(
        guardRunner = NavigationGuardRunnerImpl(guards),
        backDispatcher = BackDispatcherImpl(),
        argumentPruner = arguments,
        events = NavigationEventSink { event -> sinks.forEach { sink -> sink.emit(event) } },
    )

    val providedValues: List<ProvidedValue<*>> = listOf(
        LocalNavigationHostRenderer provides renderer,
        LocalNavigationResults provides results,
        LocalNavigationArguments provides arguments,
    )
}
```

The two "one object" rows above matter: the ingress a platform publishes to and the stream the root
collects must be the same bridge, and the argument store a screen writes must be the pruner the host
calls.

### The composition root

Three jobs, and they are the same in every application:

```kotlin
@Composable
fun App(graph: AppGraph) {
    // 1. Install the application-wide locals in one go.
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        // 2. Own the root back stack outside the host.
        val root: RootViewModel = viewModel { RootViewModel() }
        val backStack by root.backStack.collectAsState()

        // 3. Resolve inbound links — once, here, for the whole app.
        LaunchedEffect(graph) {
            graph.deepLinkEvents.links.collect { incoming ->
                val outcome = graph.deepLinkDispatcher.dispatch(raw = incoming.uri, source = incoming.source)
                if (outcome is DeepLinkOutcome.Navigate) {
                    root.onDeepLink(outcome.routes)
                }
            }
        }

        NavigationHost(
            params = NavigationHostParams(
                backStack = backStack,
                onBackStackChange = root::onBackStackChange,
            ),
        ) {
            for (provider in graph.graphProviders) {
                with(provider) { provide() }
            }
        }
    }
}
```

The worked version is [`SampleApp.kt`](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleApp.kt).

### The root back-stack owner

```kotlin
class RootViewModel : ViewModel() {
    private val mutableBackStack = MutableStateFlow<ImmutableList<Route>>(persistentListOf(HomeRoute))
    val backStack: StateFlow<ImmutableList<Route>> = mutableBackStack.asStateFlow()

    // A plain setter. The host writes guard corrections back through this; a callback that filters
    // or re-applies anything overwrites the correction and is corrected again.
    fun onBackStackChange(next: ImmutableList<Route>) {
        mutableBackStack.value = next
    }

    fun onDeepLink(routes: List<Route>) {
        if (routes.isNotEmpty()) {
            mutableBackStack.value = routes.toImmutableList()
        }
    }
}
```

Type the root stack as `Route`, not a sealed application type: screens arrive from feature graph
providers, and a closed type would make the root import every feature.

### Platform entry points

Links reach the app on the platform and are resolved in common code. The platform's only job is
`deepLinkIngress.publish(…)`.

**Android** — [`MainActivity.kt`](../../sample/app/src/main/kotlin/io/thernal/navkit/sample/android/MainActivity.kt),
[`AndroidManifest.xml`](../../sample/app/src/main/AndroidManifest.xml):

```kotlin
class MainActivity : ComponentActivity() {
    private val graph get() = (application as MyApplication).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity still carries its launch intent; publishing it again re-applies the link.
        if (savedInstanceState == null) {
            graph.deepLinkIngress.publish(intent)   // androidMain extension: reads intent.dataString
        }
        setContent { App(graph) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        graph.deepLinkIngress.publish(intent)
    }
}
```

Declare the activity `android:launchMode="singleTop"` so a link arriving while the app is open
reaches `onNewIntent` instead of a second activity, and add a `VIEW` intent filter with
`BROWSABLE` and your scheme.

**iOS** — [`MainViewController.kt`](../../sample/shared/src/iosMain/kotlin/io/thernal/navkit/sample/app/MainViewController.kt),
[`iOSApp.swift`](../../sample/iosApp/iosApp/iOSApp.swift), [`Info.plist`](../../sample/iosApp/iosApp/Info.plist):

```kotlin
private val graph: AppGraph by lazy { createGraph<AppGraph>() }

fun MainViewController(): UIViewController {
    return ComposeUIViewController { App(graph) }
}

fun handleDeepLink(url: String): Boolean {
    return graph.deepLinkIngress.publish(uri = url, source = DeepLinkSource.EXTERNAL_LINK)
}
```

```swift
WindowGroup {
    ContentView()
        .onOpenURL { url in _ = MainViewControllerKt.handleDeepLink(url: url.absoluteString) }
}
```

`Info.plist` needs `CFBundleURLTypes` for the scheme, and `CADisableMinimumFrameDurationOnPhone`
set to `true` — Compose Multiplatform checks that key on its first frame and the app closes at
launch without it.

### Previews

Every local has a harmless default, so a screen previews without a graph: the renderer draws
nothing, `LocalNavigator` discards commands, the results and argument stores hold nothing, and the
back dispatcher consumes nothing. The host itself renders nothing in inspection mode.

## Routes

```kotlin
sealed interface CheckoutRoute : Route

data object CheckoutAmount : CheckoutRoute
data object CheckoutAddress : CheckoutRoute
data class CheckoutReceipt(val orderId: String) : CheckoutRoute
```

- **Implement `Route`.** It extends Navigation3's `NavKey`, so a feature never imports Navigation3
  to declare a destination.
- **Carry identifiers, not objects.** A route crosses module boundaries and may be persisted by its
  owner; a repository, a large model or a platform object does not belong in it. Anything bigger
  than an id goes in a repository, with the id in the route.
- **Seal the routes of one flow.** A sealed type makes `when` exhaustive and gives guards and
  argument scopes one name for the whole flow: `it is CheckoutRoute`.
- **Put public routes in the owning feature's `api` module** — the ones other features navigate
  to — and keep internal steps in its `impl`.
- **Mark capabilities with interfaces named after the guard that reads them**: routes marked
  `AuthGuarded` are guarded by `AuthGuard`. See [Guards](#guards).
- **Implement `TransientRoute`** for a route that must not outlive the composition that put it
  there, such as a guard's placeholder. A host drops transient routes from the stack it is handed
  when it first composes.
- **Keep routes in a stack distinct.** Navigation3 keys an entry's saved state by its content key —
  `route.toString()` by default — so two equal routes in one stack share one state. `navigate(route)`
  returns to an entry already in the stack instead of pushing a duplicate; `RouteGuard`
  de-duplicates its rewrites, and the guard runner rejects a guard that introduces a duplicate.

The kit does not persist a back stack; its owner decides whether to. An owner that saves its stack
across process death needs routes it can serialize — one more reason to keep them small.

## Mounting a host

```kotlin
@Composable
fun <R : Route> NavigationHost(
    params: NavigationHostParams<R>,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<R>.() -> Unit,
)
```

| `NavigationHostParams` | Default | Meaning |
|---|---|---|
| `backStack: ImmutableList<R>` | required | the stack to render; the caller owns it |
| `onBackStackChange: (ImmutableList<R>) -> Unit` | required | receives every write: commands, guard corrections, settled deferrals. A plain setter. |
| `transitionSpec` | `NavAnimations.push()` | forward animation |
| `popTransitionSpec` | `NavAnimations.pop()` | back animation |
| `predictivePopTransitionSpec` | `NavAnimations.predictivePop()` | predictive back gesture animation |
| `decorators: ImmutableList<NavEntryDecorator<R>>` | empty | added after the host's own saveable-state and ViewModel-store decorators |
| `sceneStrategies: ImmutableList<SceneStrategy<R>>` | empty | consulted before the built-in bottom-sheet and single-pane strategies |
| `guards: ImmutableList<NavigationGuard>` | empty | guards for this host only, applied after the application-wide ones |
| `fallback: ((R) -> NavEntry<R>)?` | `null` | rendered for a route this host has no entry for; `null` throws instead |

### Registering screens

```kotlin
NavigationHost(params = params) {
    navEntry<CheckoutAmount> { AmountScreen() }
    navEntry<CheckoutReceipt> { route -> ReceiptScreen(orderId = route.orderId) }
    bottomSheetEntry<CouponSheet> { CouponSheetContent() }
}
```

`navEntry` registers a single-pane route, `bottomSheetEntry` one that renders in the bottom-sheet
scene. Navigation3's own `entry<K>` works as well. Three rules come straight from Navigation3:

- **Every route that can appear in this host's stack needs an entry**, including routes nobody in
  the host pushes but a guard substitutes (a sign-in screen) or a deferral shows (a placeholder).
  A missing one fails — `No entry is registered for <route> in this NavigationHost …` — unless the
  host was given a [`fallback`](#fallback-entries).
- **Register each route class once per host.** A second registration fails with an
  `IllegalArgumentException`, `` An `entry` with the same `clazz` has already been added ``. Two
  graph providers registering the same route into one host is the usual way to hit it.
- **Never hand a host an empty stack** — `NavDisplay backstack cannot be empty`.

Keep the `entries` lambda free of unstable captures. The host memoizes the entry table on the lambda,
and a lambda Compose cannot memoize rebuilds every feature's entries on every recomposition.

### Feature-owned graphs

A feature registers its own screens so the composition root never imports them:

```kotlin
class ProfileGraph(private val repository: ProfileRepository) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<ProfileRoute.Overview> { ProfileScreen(repository) }
        navEntry<ProfileRoute.Edit> { route -> EditProfileScreen(route.profileId, repository) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface ProfileBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideProfileGraph(repository: ProfileRepository): NavigationGraphProvider {
            return ProfileGraph(repository)
        }
    }
}
```

The root calls each provider inside its `NavigationHost` (see [the composition root](#the-composition-root)).
A provider's receiver is `EntryProviderScope<Route>`, so providers plug into a host whose stack is
typed `Route`; a nested host over a sealed flow type registers its entries inline.

### Fallback entries

`NavigationHostParams.fallback` is what a host renders for a route it has no entry for:

```kotlin
private fun unknownRouteEntry(route: Route): NavEntry<Route> {
    return NavEntry(key = route) { unknown -> UnknownRouteScreen(route = unknown) }
}

NavigationHostParams(
    backStack = backStack,
    onBackStackChange = root::onBackStackChange,
    fallback = ::unknownRouteEntry,
)
```

Without one, the host throws and names the route and the two causes nobody registers on purpose — a
guard's substitute and a deferral's placeholder. That is the right answer while a feature is being
built, and the wrong one in a shipped app, where the route usually arrives from outside it: a deep
link to a screen this build does not have, a notification from a newer server, a guard rewriting a
nested host's stack.

- **The root host is the one that needs it** — that is where an unknown route lands.
- **A nested host can render nothing**: `fallback = { route -> NavEntry(route) { } }`.
- **Pass a stable reference**, as above, rather than a lambda written inline — the host reads it
  through a state box, but a new lambda per frame also makes `NavigationHostParams` unequal per frame.
- A fallback hides a missing registration, so keep the screen loud enough to notice in a debug build.

### What the host does for you

- **Resolves what it is handed.** The stack in `params.backStack` goes through the guards before
  `NavDisplay` sees it, so a refused route never renders; the correction is written back through
  `onBackStackChange` a frame later. This is what guards a deep link the root applied directly.
- **Builds commands on its newest write.** The owner's `StateFlow` hands a write back a frame
  later; the host keeps its unconfirmed writes, so `popBack(2)`, or a pop followed by a push in one
  click handler, compose correctly.
- **Drops `TransientRoute`s** from the stack it is handed when it first composes.
- **Revalidates** its stack whenever a guard's `invalidations` emits, for as long as it is composed.
- **Awaits deferrals** — the only thing in the kit that does.
- **Prunes arguments** after every stack change, if it is the outermost host.
- **Provides** `LocalNavigator` (its own) and `LocalBackDispatcher` to its content.

## Navigator

```kotlin
@Composable
fun ProductScreen(productId: String) {
    val navigator = LocalNavigator.current
    Button(onClick = { navigator.push(ReviewsRoute(productId)) }) { Text("Reviews") }
}
```

`LocalNavigator.current` is the navigator of the nearest enclosing host. Read it in the composable;
do not store it in a ViewModel or a singleton — it belongs to one host and one composition.

### Commands

| Command | Does | Returns |
|---|---|---|
| `push(route)` | appends `route` | `NavigationOutcome` |
| `navigate(route, predicate = null)` | pops back to the last route matching `predicate` (default: equal to `route`); pushes `route` if nothing matches. One stack write. | `NavigationOutcome` |
| `replace(route)` | replaces the top route | `NavigationOutcome` |
| `replaceAll(route)` / `replaceAll(routes)` | replaces the whole stack; `routes` must not be empty | `NavigationOutcome` |
| `buildStack { … }` | any edit of a `MutableList<Route>` | `NavigationOutcome` |
| `popBack(force = false)` | asks the [back dispatcher](#back-handling) first; otherwise removes the top route when two or more remain | `true` if an interceptor consumed back or the stack moved |
| `popBack(count)` | `popBack()` `count` times, each asking the dispatcher | `true` if any of them did |
| `popBackTo(inclusive = false, predicate)` | pops to the last route matching `predicate`; `inclusive` removes it too. Never empties the stack. Does **not** ask the dispatcher — it is a jump, not a back. | `true` only if the stack moved |
| `canPop()` | — | more than one route in the stack |

Aliases in `NavigatorExtensions`: `pop()`, `pop(count)`, `popTo(inclusive, predicate)`,
`reset(root)`, `reset(routes)`.

`popBackTo` answers `false` when nothing matches, when the match is already on top, and when a
guard refuses the jump. `popBack(force = true)` also removes the last route — and a host cannot
render an empty stack, so only an owner that then unmounts or replaces the host should ask for it.

Every command — pops and `buildStack` included — goes through the guards before anything is
written.

### Outcomes

```kotlin
when (val outcome = navigator.push(CheckoutAmount)) {
    is NavigationOutcome.Applied -> Unit                           // exactly as asked
    is NavigationOutcome.Rewritten -> showMessage(outcome.reason?.message) // refused, redirected or trimmed
    is NavigationOutcome.Deferred -> Unit                          // a guard is deciding; the host applies its answer
}
```

Each outcome carries `stack`, the stack now in effect. `Rewritten.reason` is the guard's
[`BlockReason`](#blockreason). Read the outcome where the command is issued; if what you do with it
must outlive the screen — a command usually takes its screen away — record it somewhere that
outlives the screen, as the [order flow](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) does.

### Calling from a ViewModel

A ViewModel decides; the composable executes against `LocalNavigator`. Model navigation as an
effect stream:

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

A host's navigator keeps its identity across recompositions, so the effect is not restarted by them.

## Guards

A guard decides which back **stacks** may exist — not which route may be pushed:

```kotlin
fun interface NavigationGuard {
    val invalidations: Flow<Unit> get() = emptyFlow()
    fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict
}
```

`old` is the stack in effect, `new` the one proposed; the answer is the stack that may exist.

| Verdict | Means |
|---|---|
| `GuardVerdict.Resolved(new)` | allow |
| `GuardVerdict.Resolved(old, reason)` | refuse — this also refuses a pop |
| `GuardVerdict.Resolved(anythingElse, reason)` | rewrite: redirect, insert a step, drop a route |
| `GuardVerdict.Deferred(meanwhile) { navigator -> … }` | cannot answer yet; `meanwhile` exists until it can |

Guards are evaluated for every navigator command, for every stack a host is handed directly (a
deep link, a restored stack), and again whenever a guard's `invalidations` emits.

### Choosing the kind

| The rule is about… | Write | Sample |
|---|---|---|
| which destinations may be on the stack | `RouteGuard<Marker>` | `AuthGuard` |
| leaving a screen — the difference between two stacks | `NavigationGuard` comparing `old` and `new` | `UnsavedWorkGuard` |
| something you cannot know synchronously | either, returning `GuardVerdict.Deferred` | `PinGuard` |

### Destination rules with RouteGuard

```kotlin
interface AuthGuarded : Route

data object SignInRequired : BlockReason {
    override val message: String = "Sign in to continue"
}

data class SignInRoute(val next: Route?) : Route

class AuthGuard(private val session: SessionStore) : RouteGuard<AuthGuarded>({ it as? AuthGuarded }) {
    override val reason: BlockReason = SignInRequired
    override val invalidations: Flow<Unit> = session.changes

    override fun redirect(route: AuthGuarded, stack: ImmutableList<Route>): Route? {
        if (session.signedIn.value) {
            return null                     // let it stand
        }
        return SignInRoute(next = route)    // substitute it, remembering where the user was going
    }
}

data object AccountRoute : Route, AuthGuarded
```

- The constructor's narrowing ties the guard to its marker, so a guard cannot silently apply to
  every route in the app.
- It judges **every** matching route in the proposed stack, not only the ones entering. "Account
  requires a session" holds however `Account` got there — which is also what makes it correct when
  a stack is revalidated in place.
- It answers by **substitution**: the rejected route is replaced where it sits, and two rejected
  routes redirecting to the same destination collapse into one.
- Carrying `next` keeps the user's intent. After signing in, the sign-in screen continues with
  `navigator.replace(next)`.

### Transition rules

A rule about leaving a screen compares the two stacks, so it implements `NavigationGuard`:

```kotlin
class UnsavedWorkGuard(private val drafts: DraftStore) : NavigationGuard {
    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        val isLeavingEditor = old.any { it is EditorRoute } && new.none { it is EditorRoute }
        if (isLeavingEditor && drafts.hasUnsavedChanges) {
            return GuardVerdict.Resolved(stack = old, reason = UnsavedWork)
        }
        return GuardVerdict.Resolved(new)
    }
}
```

It refuses every way out — the system gesture, a Back button, `popBackTo`, `replaceAll`, a deep
link — because each is a stack change. **Keep it about one screen's own routes.** `old` does not
move while the runner folds, so a rule that rejects *any* difference between the stacks undoes every
rewriting guard, which rewrites again, and the runner fails at its round limit.

### Deciding later

```kotlin
class PinGuard(private val session: PinSession) : NavigationGuard {
    override val invalidations: Flow<Unit> = session.changes

    override fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict {
        if (!session.locked.value || new.none { it is PinProtected }) {
            return GuardVerdict.Resolved(new)
        }
        return GuardVerdict.Deferred(meanwhile = withPrompt(old)) { _ ->
            if (session.awaitUnlock()) {           // suspends; unlocking flips `locked` to false
                GuardVerdict.Resolved(new)         // continue where the user was going
            } else {
                GuardVerdict.Resolved(stack = old, reason = PinRequired)
            }
        }
    }

    // PinEntryRoute is a TransientRoute, registered in the host — and never added twice
    private fun withPrompt(stack: ImmutableList<Route>): ImmutableList<Route> {
        if (PinEntryRoute in stack) {
            return stack
        }
        return (stack + PinEntryRoute).toImmutableList()
    }
}
```

`meanwhile` is what exists until the answer arrives: `old` holds the navigation, `old` plus a
placeholder shows one. Returning `Resolved(new)` continues to the destination the user asked for,
which is the point of deferring rather than redirecting. The rules:

- **Answer synchronously once settled.** The settled stack is applied and guarded again; a guard
  that defers a second time for the same stack never converges. Cache the answer — above,
  `awaitUnlock` changes `locked` before it returns.
- **Only a mounted host awaits a deferral**, one at a time, keyed on the stack that was attempted —
  a double tap or a revalidation does not start a second wait. Unmounting the host cancels it.
  `NavigationGuardRunner.resolve` collapses a deferral to `meanwhile` and starts nothing.
- **A wait is abandoned when the stack moves without it** — the user backs out of the placeholder,
  a deep link arrives, the host is handed another list. Its answer is then discarded. The navigator
  given to `resolve` is exempt: its pushes are part of the wait.
- **Put the placeholder in `meanwhile`.** Pushing it from `resolve` also works — that navigator's pushes are part of the wait — but it is a second stack change, and when `meanwhile` has just removed the screen on top the user sees a pop followed by a push.
- **Make the placeholder a `TransientRoute`.** A coroutine does not survive process death or a
  recreated composition; a restored stack showing a prompt with nothing left to answer it would be a
  screen nobody can leave. The host drops transient routes from the stack it first composes.
- **`meanwhile` obeys the runner's rules** below — for instance, dropping protected routes may not
  empty the stack.

### Staying correct when the answer changes

A guard whose answer depends on something that moves — a session, a role, a flag — overrides
`invalidations`. Every mounted host collects it and revalidates its own stack in place, so a route
that has *become* invalid leaves without anyone navigating:

```kotlin
class SessionStore {
    private val state = MutableStateFlow(false)
    val signedIn: StateFlow<Boolean> = state.asStateFlow()
    val changes: Flow<Unit> = state.drop(1).map { }   // drop(1): a StateFlow replays its value on collect
}
```

Make the flow hot (a `StateFlow`- or `SharedFlow`-derived one): every mounted host collects it, so
a cold flow that does work per collector does it once per host.

### BlockReason

`BlockReason` is an empty vocabulary the application fills in — the kit cannot know whether a
refusal reads as a sign-in prompt, a paywall or an error. It is reported on
`NavigationOutcome.Rewritten.reason` and `NavigationEvent.Blocked.reason`.

### Registering guards

- **Application-wide:** contribute `@IntoSet NavigationGuard` (or pass it to
  `NavigationGuardRunnerImpl`). It applies to **every** host, nested ones included.
- **One host:** `NavigationHostParams(guards = persistentListOf(WizardGuard(state)))` — for a
  flow's internal rules, or a guard whose dependencies live in a scope the application graph cannot
  reach. These run after the application-wide guards.
- **Register what a guard can put on the stack** — its substitute and its placeholder — in every
  host whose stack the guard can rewrite, or that host fails on it (or renders its
  [`fallback`](#fallback-entries)).

### How the runner resolves a stack

1. Guards are folded in order, each seeing the previous one's output as `new`; `old` stays the stack
   that was in effect.
2. The fold repeats until a whole round rewrites nothing, so a route a guard introduced is guarded
   like any other. After 8 rounds it fails: `Navigation guards did not settle after 8 rounds`.
3. A deferral stops the fold and is returned.
4. Each verdict is checked. A guard may drop and insert routes, but a verdict that **empties** a
   non-empty stack, **reorders** the routes it kept, or **introduces a duplicate** fails with an
   `IllegalStateException` naming the guard.

`evaluate` must therefore be **pure and cheap**: it runs once per guard per round, on every command,
on every handed stack and on every invalidation.

## Passing data between screens

| Need | Use | Survives process death |
|---|---|---|
| an id the next screen needs | a property on its route | if the owner saves the stack |
| a value handed back to a screen already on the stack | a [result](#results) | no |
| one value read or updated by several screens about to open | an [argument](#arguments) | no |
| a flow's shared state that must survive process death | a ViewModel on the flow's entry with `SavedStateHandle`, the flow mounted as a [nested host](#nested-hosts-and-tabs) | yes |
| anything large | a repository, with its id in the route | as the repository does |

Results and arguments are told apart by direction; using one for the other is the mistake they exist
to prevent. Both are application-scoped, reached through a composition local, and **in memory
only** — a consumer that finds nothing treats it as a first visit or restarts its flow, never as an
error.

### Results

```kotlin
// Declared once, next to the producing feature's routes. Namespace the name.
val SelectedColour = resultKey<String>("palette.selected_colour")

// The closing screen
val navigator = LocalNavigator.current
val results = LocalNavigationResults.current
Button(onClick = {
    results.post(key = SelectedColour, value = "Teal")
    navigator.popBack()
}) { Text("Teal") }

// The screen it returns to
var colour by rememberSaveable { mutableStateOf<String?>(null) }
ResultEffect(SelectedColour) { picked -> colour = picked }
```

| `NavigationResults` | |
|---|---|
| `post(key, value)` | leaves a value; a second post before consumption replaces the first |
| `consume(key)` | takes and removes it — one delivery |
| `clear(key)` | drops it undelivered, for a flow abandoned rather than completed |
| `pending: StateFlow<Set<String>>` | the names waiting — names only, so one feature's values are not readable by every screen |

- **`ResultEffect` runs when the waiting screen is composed.** Navigation3 composes only the current
  scene, so a covered screen receives its result when it is uncovered; a result posted while it is
  on top arrives immediately.
- **Store what the result updates in `rememberSaveable` or an entry-scoped ViewModel**, not in
  `remember`: the waiting screen leaves composition while it is covered, and `remember` forgets.
  Forward to the state holder: `ResultEffect(ReviewOutcome, onResult = viewModel::onDecision)`.
- **A flow can post and leave in one step** — `post` then `popBackTo { it is LauncherRoute }` — and
  the launcher is uncovered with the answer waiting. The mailbox is application-scoped, so this also
  works when the flow is a nested host.
- Two keys with **the same name and different types** fail when read —
  `` Result `x` was posted as … and read as … ``. A blank name fails when the key is created.

### Arguments

```kotlin
val CheckoutDraftKey = argumentKey<CheckoutDraft>("checkout.draft")

// The screen that starts the flow: put, then push, in the same action
val navigator = LocalNavigator.current
val arguments = LocalNavigationArguments.current
Button(onClick = {
    arguments.put(
        key = CheckoutDraftKey,
        value = CheckoutDraft(),
        scope = whileInStack { it is CheckoutRoute },
    )
    navigator.push(CheckoutAmount)
}) { Text("Start checkout") }

// Any screen of the flow
val draft = LocalNavigationArguments.current.get(CheckoutDraftKey)
```

| `NavigationArguments` | |
|---|---|
| `put(key, value, scope)` | stores a value for as long as `scope` says; a second put replaces it |
| `get(key)` | reads without removing — several screens read the same argument |
| `remove(key)` | drops it before its scope would |

| Scope | Alive while |
|---|---|
| `whileInStack { route -> … }` | any route in the stack matches |
| `whileRouteInStack<CheckoutRoute>()` | any route of that type is in the stack |
| `ArgumentScope { stack -> … }` | your own question about the stack |

**Lifetime is derived from the stack, never counted.** The outermost host calls
`ArgumentPruner.pruneFor(stack)` after every change of its stack:

- An argument whose scope **has never been alive** is kept until it is — so the stack change that
  starts a flow does not delete the value it was started with. It is dropped the first time it has
  been alive and then is not.
- A put whose scope **is already alive** (a flow updating its own argument) counts as alive from the
  start, so it still dies with the flow.
- Scope to the **flow's sealed type**, not one screen: back, `popBackTo`, a guard rewrite and a deep
  link then all end the argument correctly without being handled separately.
- An argument put for a flow that never starts is never alive and never pruned — `remove` it.
- Only the **outermost** host prunes, against **its own** stack. Scope an argument to routes of that
  stack — for a flow mounted as a nested host, the route that mounts it. An argument scoped to routes
  that exist only inside a nested stack is never alive at the root and is never pruned.
- **The store is not snapshot state.** A text field bound straight to `get(…)` never recomposes and
  loses every keystroke. Keep the field in its own `rememberSaveable` state and write each change
  through, applying it to the current value:
  `arguments.put(key, change(arguments.get(key) ?: Draft()), scope)`.
- Same name with different types fails when read. A blank name fails when the key is created.
- Never prune from a guard: `evaluate` must stay pure, and runs several times per navigation.

## Back handling

Back reaches the stack in two ways, and both end in `Navigator.popBack()`:

- the **system** back gesture or button, which the host routes to `popBack()`;
- an **in-app** Back button calling `navigator.popBack()`.

`popBack()` asks the `BackDispatcher` first. A callback that returns `true` consumes back and
nothing is popped. Callbacks run newest first.

```kotlin
@Composable
fun DraftScreen() {
    val navigator = LocalNavigator.current
    var text by rememberSaveable { mutableStateOf("") }
    var isAsking by rememberSaveable { mutableStateOf(false) }

    // impl: io.thernal.navkit.navigation.impl.presentation.back.NavigationBackHandler
    NavigationBackHandler(enabled = text.isNotBlank()) { isAsking = true }

    if (isAsking) {
        DiscardDialog(
            onDiscard = {
                isAsking = false
                // popBackTo skips the dispatcher; popBack() would be intercepted again.
                navigator.popBackTo(inclusive = true) { it is DraftRoute }
            },
            onKeep = { isAsking = false },
        )
    }
}
```

A module that should not see `impl` registers through the `api` contract directly:

```kotlin
@Composable
fun InterceptBack(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher = LocalBackDispatcher.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnBack by rememberUpdatedState(onBack)
    DisposableEffect(dispatcher) {
        val registration = dispatcher.register(
            BackCallback {
                if (currentEnabled) {
                    currentOnBack()
                }
                currentEnabled   // true consumes back
            },
        )
        onDispose { registration.close() }
    }
}
```

- **A handler hears back only while its screen is composed**, and only through `popBack`. It does
  not see `popBackTo`, `replace`, `replaceAll`, `navigate`, `buildStack`, a deep link or a guard
  rewrite. To refuse *every* way out, write a [transition guard](#transition-rules).
- **Letting back through from inside the handler:** calling `navigator.popBack()` inside the
  callback pops — a dispatch nested in another consumes nothing.
- **After a confirmation:** disable the handler first, or leave with `popBackTo`, which does not
  consult the dispatcher.
- One `BackDispatcher` serves every host; see [Known limitations](#known-limitations).

## Deep links

```
platform ──publish(uri, source)──▶ DeepLinkIngress ═ DeepLinkEvents.links (buffered)
                                                          │ collected by the root
                                                          ▼
                     DeepLinkDispatcher.dispatch(raw, source) ─ parse ─▶ handler owning the page
                                                          │
                                   DeepLinkOutcome ◀──────┘
                                         │ Navigate(routes)
                                         ▼
                        root.onDeepLink(routes) ─▶ host guards the stack ─▶ rendered
```

### Link bases

A `DeepLinkBase` is a prefix the application's links start with: an app scheme, or a web origin with
an optional path. The application contributes one for every scheme and domain it answers to:

```kotlin
@Provides
@IntoSet
fun provideAppSchemeBase(): DeepLinkBase {
    return DeepLinkBase("navkit://")
}
```

**A link is read by removing the most specific registered base it starts with; what follows is the
page and its segments.** Every registered form of a link therefore reaches the same handler, and a
feature declares its page once.

| Registered bases | Raw link | `base` | `pathSegments` | `page` |
|---|---|---|---|---|
| `navkit://` | `navkit://orders/77` | `navkit://` | `orders`, `77` | `orders` |
| `https://example.com` | `https://example.com/orders/77` | `https://example.com` | `orders`, `77` | `orders` |
| `https://example.com`, `https://example.com/app` | `https://example.com/app/orders` | `https://example.com/app` | `orders` | `orders` |
| `navkit://` | `navkit://search?q=bar%20table&tag=a&tag=b` | `navkit://` | `search` | `search`; `q` → `bar table`, `tag` → `a`, `b` |
| `https://example.com` | `https://elsewhere.example/orders/77` | — | — | none: `NotFound` |
| `https://example.com` | `https://example.com` | — | — | none: `NotFound` |

- On an app-scheme base the host is the first page: `navkit://orders` has no domain to be a host.
- The scheme and the host compare case-insensitively; a base's path compares whole segments, exactly —
  `https://example.com/app` does not match `https://example.com/apple/…`.
- A base carries no query or fragment; a web base (`http`, `https`) names a host; a base without a host
  has no path. Anything else fails when the base is created.
- A link that starts with **no** registered base resolves to `NotFound`: a domain the application does
  not own never reaches a handler. Handlers registered with no base at all fail when the dispatcher is
  built (`Deep link handlers are registered but no DeepLinkBase is…`).
- **Keep the bases in step with the platform declarations** — the schemes in the Android intent filters
  and `CFBundleURLTypes`, the verified domains. A scheme the platform delivers but nothing registers
  resolves to `NotFound`.

`DeepLink` exposes `raw`, `base` (the base that matched), `scheme`, `host` (as written — on an app
scheme, the page), `pathSegments`, `query`, `page` (the first segment) and `query(key)` (the first
value). Segments and values arrive decoded.

### Handlers

Each page has exactly **one** owning handler; two handlers claiming a page fail when the dispatcher is
built (`Deep link page 'x' is claimed by A and B`), and a blank page fails too.

```kotlin
class OrdersDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("orders")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        if (request.source == DeepLinkSource.IN_APP_NOTIFICATION) {
            return DeepLinkOutcome.Rejected(reason = "In-app notifications do not open orders")
        }
        val id = request.deepLink.pathSegments.getOrNull(1)
        val base = listOf(HomeRoute, OrdersRoute)
        if (id == null) {
            return DeepLinkOutcome.Navigate(routes = base)
        }
        return DeepLinkOutcome.Navigate(routes = base + OrderRoute(id = id))
    }
}
```

With several pages, declare them as an enum and let `TypedDeepLinkHandler` derive `pages`:

```kotlin
enum class ProfilePage(override val page: String) : DeepLinkPage {
    View("profile"),
    Edit("profile-edit"),
}

class ProfileDeepLinkHandler : TypedDeepLinkHandler<ProfilePage>(ProfilePage.entries) {
    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val name = request.deepLink.page ?: return DeepLinkOutcome.NotFound
        val page = pageOf<ProfilePage>(name) ?: return DeepLinkOutcome.NotFound
        val id = request.deepLink.query("id") ?: return DeepLinkOutcome.Rejected("missing id")
        return when (page) {
            ProfilePage.View -> DeepLinkOutcome.Navigate(listOf(HomeRoute, ProfileRoute.Overview(id)))
            ProfilePage.Edit -> DeepLinkOutcome.Navigate(listOf(HomeRoute, ProfileRoute.Edit(id)))
        }
    }
}
```

| `DeepLinkOutcome` | |
|---|---|
| `Navigate(routes)` | the **whole stack** to show. Keep the app's root at the bottom, so back goes somewhere sensible instead of out of the app. |
| `Rejected(reason)` | the page is ours, the request is not acceptable |
| `NotFound` | not a link, no registered base, no page after the base, or no handler owns the page |

- **Return a stack, not a destination.** A link to an order leaves the order list behind it.
- **Do not check access in a handler.** A resolved stack is guarded by the host before it renders;
  a link to a protected route becomes a sign-in screen on its own.
- **Use `source` to decide trust.** `EXTERNAL_LINK`, `PUSH_NOTIFICATION` and `IN_APP_NOTIFICATION`
  need not open the same things.
- **Validate what the link carries.** It comes from outside the app.

### Resolving at the root

Resolving and applying a link is the job of the single root state holder, not of a host: a host can
be mounted anywhere, but there is one link stream per app. The root applies `Navigate(routes)`
through its own stack setter — see [the composition root](#the-composition-root). The ingress's
`Boolean` only says the link was queued (`false` for a blank URI or a full buffer); what a handler
decided arrives later, at the root. Record it there if a screen needs to show it, as the sample's
[`DeepLinkLog`](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/DeepLinkLog.kt) does.

The bridge is buffered, so a link published at cold start — before the root collects — still
arrives.

### Platform setup

See [Platform entry points](#platform-entry-points): Android publishes `intent` from `onCreate`
(first creation only) and `onNewIntent`, with a `singleTop` activity and a `VIEW`/`BROWSABLE` intent
filter; iOS publishes from SwiftUI's `onOpenURL`, with the scheme in `CFBundleURLTypes`. Every scheme
and domain declared there is also registered as a [`DeepLinkBase`](#link-bases).

```sh
adb shell am start -a android.intent.action.VIEW -d navkit://orders/77
xcrun simctl openurl booted navkit://orders/77
```

### Outbound links

```kotlin
val appScheme = DeepLinkBase("navkit://")
val web = DeepLinkBase("https://example.com")

buildDeepLinkUri(base = appScheme, page = "orders", query = mapOf("tab" to "open"))
// navkit://orders?tab=open

ProfilePage.View.buildUri(base = web, query = mapOf("id" to "42"))
// https://example.com/profile?id=42
```

Building appends the page to a base and parsing removes the base, so a link built on a registered base
is read back as the same page — on an app scheme the page takes the host's position. Share the
`DeepLinkBase` values the application registers rather than repeating the strings.

## Nested hosts and tabs

A `NavigationHost` can be mounted inside an entry of another — for tabs, a wizard, a flow with its
own back stack. Its stack is owned the same way, usually by a ViewModel scoped to the entry that
mounts it.

```kotlin
@Composable
fun CheckoutFlowScreen() {
    val flow: CheckoutFlowViewModel = viewModel { CheckoutFlowViewModel() }
    val steps by flow.steps.collectAsState()
    val outer = LocalNavigator.current           // read before mounting: inside, this is the flow's

    NavigationHost(
        params = NavigationHostParams(backStack = steps, onBackStackChange = flow::onStepsChange),
    ) {
        navEntry<CheckoutAmount> { AmountStep() }
        navEntry<CheckoutReceipt> { route -> ReceiptStep(route, onDone = { outer.popBack() }) }
    }
}
```

- **Inside, `LocalNavigator` is the nested host's.** A screen's `push` lands in the innermost stack.
  To command the outer host, read `LocalNavigator.current` before mounting and pass it down.
- **Application-wide guards apply to nested stacks too.** Register their substitutes in the nested
  host (a sign-in screen), or that host fails on them. Rules for the flow alone go in
  `NavigationHostParams.guards`.
- **Only the outermost host prunes arguments.** Scope a flow's arguments to the route that mounts it.
- **Results work across hosts** — the mailbox is application-scoped.
- **System back** is handled by the innermost host that has something to pop; the kit's
  `BackDispatcher`, which `popBack()` consults, is shared by all hosts.
- **Keep nesting shallow** — one level for a flow. Each mounted host carries its own `NavDisplay`,
  saved-state holder, ViewModel stores and guard revalidation.

### Two shapes of tabs

| | One host, tabs as its stack | A stack per tab |
|---|---|---|
| Owner | one `ImmutableList<Route>` | `Map<Tab, ImmutableList<Route>>` plus the selected tab |
| Select a tab | replace the stack with the tab's root | hand the host that tab's list |
| Depth after switching away and back | gone | kept |
| Re-selecting the active tab | — | reset it to its root |
| Sample | [`SingleHostTabsScreen.kt`](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/tabs/SingleHostTabsScreen.kt) | not in the sample |

**Entry state does not survive a tab switch in either shape.** Navigation3 treats an entry that
leaves the list it is handed as popped, and a pop clears that entry's `rememberSaveable` state and
its entry-scoped ViewModels. With one host that is obvious — selecting a tab replaces the stack. With
a stack per tab it is the part that surprises people: the hidden tab's **routes** survive, because
they live in the owner, but its entries' state does not.

So scope a tab's state to the entry that **mounts** the tabs, not to the tab's own entry. A host
publishes that owner as `LocalHostViewModelStoreOwner`, resolved like `LocalNavigator` to the nearest
host — it is read before `NavDisplay`, where `LocalViewModelStoreOwner` is still the mount point
rather than an entry's own:

```kotlin
// inside the tab's entry
val model: SettingsTabViewModel = viewModel(
    viewModelStoreOwner = checkNotNull(LocalHostViewModelStoreOwner.current),
    key = "settings",
) { SettingsTabViewModel() }
```

That is the lifetime a tab has: alive while the tabs screen is on the stack above, cleared with it.
Entry scoping is right for a pushed detail, which should die when it is popped — so push details onto
the host **above** the bar and keep each tab one entry deep.

Keeping a hidden tab's entry state alive means keeping that tab's entries alive, which is
Navigation3's `rememberDecoratedNavEntries` per tab feeding one `NavDisplay` — a shape this kit does
not expose today, because `NavigationHost` builds its decorators and its display together.

## Bottom sheets, scenes and transitions

### Bottom sheets

```kotlin
bottomSheetEntry<CouponSheet> { CouponSheetContent() }
```

The built-in scene claims the run of consecutive sheet entries at the top of the stack, so a sheet
that pushes another sheet stays one surface: back steps through the sheet's own entries before it
closes. The scene is a bare container on purpose — no scrim, drag handle or outside-tap dismiss;
those belong to your design system. `bottomSheetEntry` only adds the `BOTTOM_SHEET_METADATA_KEY`
metadata.

### The surface

Supply the surface once, on the host, instead of letting each step bring its own:

```kotlin
val AppSheet: BottomSheetContainer = { dismiss, step ->
    Box(Modifier.fillMaxSize().background(scrim).clickable(onClick = dismiss)) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .animateEnterExit(
                    enter = slideInVertically { height -> height },
                    exit = slideOutVertically { height -> height },
                ),
        ) {
            Column { DragHandle(); step() }
        }
    }
}

NavigationHostParams(…, bottomSheetContainer = AppSheet)
```

Hold it in a `val` — these params are memoized by equality, and written inline it is a new lambda
every frame. Left unset, the host draws the steps bare and each one has to bring its own surface.

The container runs inside the host's `AnimatedVisibility`, which fades the whole sheet — scrim
included — and is the `AnimatedVisibilityScope` the panel's `animateEnterExit` comes from. So the
sheet rises from the bottom edge while the backdrop only fades, and the library never has to guess
which of your layers is the panel. `dismiss` closes every step of the run at once, which a surface
written against no feature cannot work out for itself.

One surface for every step is also what makes the step-to-step transition possible: the steps swap
*inside* the panel, and `AnimatedContent`'s size transform makes its **height animate** from one
step's content to the next instead of snapping.

`NavDisplay`'s own transitions never run for an overlay, so this is where a sheet's own animation
lives. Closing waits for it before the entry leaves composition — what `OverlayScene.onRemove` is
for — and input is ignored while it plays, because the routes that would receive it have already
left the stack.

The overlay is rendered as a sibling of the pane rather than inside it, so the surface you draw has
to fill the window and place the panel itself — see
[`SheetSurface`](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/ui/SheetSurface.kt)
for one that does, and the [sheets example](../../sample/shared/src/commonMain/kotlin/io/thernal/navkit/sample/sheets/README.md)
for the rest of it. A pane under an overlay stays composed, capped at `STARTED`: a result posted
from a sheet reaches the screen behind it while the sheet is still open.

### Scenes

Pass your own `SceneStrategy` through `NavigationHostParams.sceneStrategies`; yours are consulted
first, then the bottom-sheet strategy, then single-pane, which claims everything left.

### Decorators

`NavigationHostParams.decorators` are appended after the two the host always installs: the
saveable-state decorator (per-entry `rememberSaveable`) and the ViewModel-store decorator (per-entry
ViewModels, cleared when the entry is popped).

### Transitions

`NavTransitionScope<R>` is `AnimatedContentTransitionScope<Scene<R>>.() -> ContentTransform`;
`PredictiveNavTransitionScope<R>` also receives the swipe edge. Leave a spec `null` for the default.

```kotlin
// impl: io.thernal.navkit.navigation.impl.presentation.host.NavAnimations
val up = remember { NavAnimations.slideTo<Route>(direction = { SlideDirection.UP }) }
val down = remember { NavAnimations.slideTo<Route>(direction = { SlideDirection.DOWN }) }

NavigationHostParams(
    backStack = steps,
    onBackStackChange = flow::onStepsChange,
    transitionSpec = up,
    popTransitionSpec = down,
)

// No animation at all
val none: NavTransitionScope<Route> = { ContentTransform(EnterTransition.None, ExitTransition.None) }
```

| `NavAnimations` | |
|---|---|
| `push(duration, easing)` | slide in from a third of the width, fade — the default forward |
| `pop(duration, easing)` | the reverse — the default back |
| `predictivePop(duration, easing)` | pop following the swipe edge — the default predictive back |
| `slideTo(direction, duration, easing)` | full-size slide in a `SlideDirection`: `LEFT`, `RIGHT`, `UP`, `DOWN`, `NONE` |

`NavigationDefaults` holds the numbers: `DURATION` 260 ms, `FADE_DURATION` 180 ms,
`PUSH_OFFSET_DIVIDER` 3, `POP_OFFSET_DIVIDER` 6, `EASING` `FastOutSlowInEasing`.

## Navigation events

Every navigator command emits a `NavigationEvent` into the `NavigationEventSink`:

| Event | Emitted when |
|---|---|
| `Push(previous, pushed)` | `push` applied; `navigate` that grew the stack |
| `Pop(popped, backTo)` | `popBack` removed a route |
| `Replace(old, new)` | `replace` applied |
| `ReplaceAll(routes)` | `replaceAll`; `buildStack`, `popBackTo` or a shrinking `navigate` that changed the stack |
| `Blocked(attempted, applied, reason)` | a guard rewrote the command's stack — emitted **instead of** the command's own event |
| `Deferred(attempted, meanwhile)` | a guard deferred |

Each has a one-line `message` and a multi-line `detail`. Only commands emit: a stack handed to the
host directly (a deep link, a revalidation) does not.

```kotlin
class ScreenTracker(private val analytics: Analytics) : NavigationEventSink {
    override fun emit(event: NavigationEvent) {
        if (event is NavigationEvent.Push) {
            analytics.screen(event.pushed::class.simpleName.orEmpty())
        }
    }
}
```

Contribute sinks `@IntoSet`; every contributed sink receives every event, and an app with none gets
`NavigationEventSink.NoOp`. A sink runs on the command's call path — keep it quick.

## Composition locals

| Local | Provided by | Without a provider |
|---|---|---|
| `LocalNavigationHostRenderer` | the root, from the graph | draws nothing |
| `LocalNavigationResults` | the root, from the graph | holds nothing |
| `LocalNavigationArguments` | the root, from the graph | holds nothing |
| `LocalNavigator` | each `NavigationHost`, to its content | discards commands: `Rewritten` over an empty stack; pops return `false` |
| `LocalBackDispatcher` | each `NavigationHost`, to its content | consumes nothing |

A screen whose every command answers `Rewritten` over an empty stack is composed outside any host.
A host that draws nothing at all has no renderer: the root did not install the graph's values.

## What survives what

| | Recomposition | Activity recreated (Android) | Process death | Entry popped |
|---|---|---|---|---|
| Root stack in a ViewModel | kept | kept | lost unless the owner saves it | — |
| A result or an argument | kept | kept, if the graph is process-scoped | lost | consumed / pruned |
| `remember` in a screen | kept while composed | lost | lost | lost |
| `rememberSaveable` in a screen | kept | kept | kept | removed |
| Entry-scoped ViewModel | kept | kept | lost — use `SavedStateHandle` | cleared |
| A guard's pending deferral | kept | lost; its transient placeholder is dropped | lost | abandoned |

Verify against process death, not only rotation: an application-scoped store survives rotation and
looks correct while it is empty after process death. iOS has no configuration change at all, so
shared code cannot lean on that survival.

## Testing

Everything except the composable host is plain Kotlin and runs in `commonTest`.

```kotlin
@Test
fun signedOutAccountBecomesSignIn() {
    val runner = NavigationGuardRunnerImpl(listOf(AuthGuard(SessionStore())))

    val verdict = runner.resolve(
        old = persistentListOf(HomeRoute),
        new = persistentListOf(HomeRoute, AccountRoute),
    )

    assertEquals(persistentListOf(HomeRoute, SignInRoute(next = AccountRoute)), verdict.stack)
    assertEquals(SignInRequired, verdict.reason)
}
```

| Subject | Build it with |
|---|---|
| a guard | `NavigationGuardRunnerImpl(listOf(guard))`; `resolve` for the synchronous answer, `resolveDeferrable` to see a `Deferred` |
| command sequences | `BackStackNavigator(buildBackStack = { b -> stack = stack.toMutableList().apply(b) }, resolveCanPop = { stack.size > 1 }, resolveGuardRunner = { runner }, backDispatcher = BackDispatcherImpl(), events = recorder)` |
| emitted events | a recording `NavigationEventSink { events += it }` |
| results | `NavigationResultsImpl()` |
| arguments | `NavigationArgumentsImpl()`, calling `pruneFor(stack)` for each stack change |
| back interception | `BackDispatcherImpl()` |
| deep-link handlers | `DeepLinkDispatcherImpl(handlers = setOf(handler), bases = setOf(base)).dispatch(raw, source)` in `runTest` |
| link parsing and building | `parseDeepLink(raw, bases)`; `buildDeepLinkUri(base, page)` read back through it |

`navigation/impl/src/commonTest` holds the kit's own tests in exactly this shape.

## Rules checklist

- [ ] Routes implement `Route`, carry identifiers only, and are distinct within a stack.
- [ ] Each host's stack is owned outside it, and `onBackStackChange` is a plain setter.
- [ ] The graph is created once per process; the root installs its provided values once.
- [ ] Screens navigate through `LocalNavigator.current`; nothing stores a navigator.
- [ ] Every route that can appear in a host — guard substitutes and placeholders included — has an
      entry in that host, registered once.
- [ ] No host is ever handed an empty stack.
- [ ] Access rules are guards, not checks at call sites; `evaluate` is pure and cheap.
- [ ] A transition guard is phrased about one screen's own routes.
- [ ] A deferring guard answers synchronously once settled, and its placeholder is a `TransientRoute`.
- [ ] Guards with changing answers expose hot `invalidations`.
- [ ] Results go backwards, arguments forwards; keys are declared once, next to the producing
      feature's routes, with namespaced names.
- [ ] Nothing that must survive process death lives only in a result or an argument.
- [ ] Arguments are put in the same action that pushes their flow, scoped to a sealed flow type in the
      outermost stack.
- [ ] Deep links are resolved at the root and return whole stacks; handlers do not check access.
- [ ] Every scheme and domain the platform declares is registered as a `DeepLinkBase`, and outbound
      links are built on those bases.
- [ ] "Refuse every way out" is a transition guard; "confirm on back" is `NavigationBackHandler`.

## Known limitations

- **Application-wide guards run on every host's stack**, nested ones included, so a nested host must
  register what those guards substitute. Scoping guards to hosts is open —
  [`docs/todos/arguments.md`](../../docs/todos/arguments.md) §3.
- **Every mounted host collects guard invalidations**, so N mounted hosts revalidate N times per
  emission.
- **One `BackDispatcher` is shared by every host.** A callback registered inside a nested host is
  also consulted by the outer host's `popBack()`.
- **Results and arguments are in memory only.**
- **Not published** to a Maven repository; `iosX64` is not a target, because `navigation3-ui` has no
  variant for it.

## API index

`api` — package prefix `io.thernal.navkit.navigation.api`.

| Package | Declarations |
|---|---|
| `presentation.model` | `Route`, `TransientRoute`, `NavigationHostParams`, `SlideDirection` |
| `presentation.host` | `NavigationHost`, `NavigationHostRenderer`, `LocalNavigationHostRenderer`, `navEntry`, `bottomSheetEntry`, `BOTTOM_SHEET_METADATA_KEY` |
| `presentation.navigator` | `Navigator`, `LocalNavigator`, `NavigationOutcome`, `NavigationGraphProvider`, `pop`, `popTo`, `reset` |
| `presentation.guard` | `NavigationGuard`, `RouteGuard`, `GuardVerdict`, `BlockReason`, `NavigationGuardRunner` |
| `presentation.result` | `ResultKey`, `resultKey`, `NavigationResults`, `LocalNavigationResults`, `ResultEffect` |
| `presentation.argument` | `ArgumentKey`, `argumentKey`, `NavigationArguments`, `LocalNavigationArguments`, `ArgumentScope`, `whileInStack`, `whileRouteInStack`, `ArgumentPruner` |
| `presentation.back` | `BackDispatcher`, `BackCallback`, `LocalBackDispatcher` |
| `presentation.deeplink` | `DeepLinkHandler`, `TypedDeepLinkHandler`, `DeepLinkOutcome`, `DeepLinkDispatcher`, `DeepLinkIngress`, `DeepLinkEvents`; `DeepLinkIngress.publish(intent)` on Android |
| `presentation.transition` | `NavTransitionScope`, `PredictiveNavTransitionScope`, `NavigationDefaults` |
| `presentation.log` | `NavigationEvent`, `NavigationEventSink` |
| `domain` | `DeepLink`, `DeepLinkBase`, `DeepLinkPage`, `DeepLinkRequest`, `DeepLinkSource`, `IncomingDeepLink`, `buildDeepLinkUri`, `DeepLinkPage.buildUri`, `pageOf` |

`impl` declarations an application reaches for — package prefix `io.thernal.navkit.navigation.impl`:

| Package | Declarations |
|---|---|
| `presentation.back` | `NavigationBackHandler` |
| `presentation.host` | `NavAnimations`, `NavigationHostRendererImpl` |
| `presentation.scene` | `BottomSheetSceneStrategy` |
| `domain.navigator` | `BackStackNavigator` |
| `domain.guard` | `NavigationGuardRunnerImpl` |
| `domain.result` | `NavigationResultsImpl` |
| `domain.argument` | `NavigationArgumentsImpl` |
| `domain.back` | `BackDispatcherImpl` |
| `domain.deeplink` | `DeepLinkDispatcherImpl`, `parseDeepLink` |
| `data` | `RuntimeDeepLinkBridge` |

`wiring` — `io.thernal.navkit.navigation.wiring.NavigationWiring`.
