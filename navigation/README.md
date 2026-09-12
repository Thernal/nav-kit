# navigation

Public Navigation3 contracts: routes, the navigator command surface, the `NavigationHost` render
contract, guards, deep links, back handling, and cross-screen results. `impl` owns every concrete
behavior; `api` never depends on it.

## Module layout

- `api` — Navigation3 routes, the navigator command surface, deep-link/guard/result/back contracts,
  the `NavigationHost` render contract, `DeepLinkIngress`, the bottom-sheet/modal route markers, and
  the `NavigationEvent` stream.
- `impl` — the Navigation3 host (internal), the `BackStackNavigator` command adapter it builds per
  host, overlay scenes, animations, deep-link dispatch and parsing, guards, results.
- `wiring` — binds the dispatchers, guard runner, result store and renderer into an application
  graph, and contributes `NavigationHostRenderer` as a `ProvidedValue`. `Navigator` is not bound
  here — it holds no state; `NavigationHost` builds and provides one per host, over the
  caller-owned `backStack`/`onBackStackChange` pair on its `NavigationHostParams`.

## Routes

`presentation.model.Route` is the marker every route implements (`interface Route : NavKey`,
`@Immutable`). Public cross-feature routes live in the owning feature's `api`; internal
wizard/tab/sheet routes stay in its `impl`. Keep payloads small identifiers, not repositories,
large models, or platform objects — routes cross module boundaries and often survive process death.

## Navigator — the command surface

`Navigator` (`push`/`pop`/`replace`/`navigate`/`popBackTo`/`popBack`) holds no back-stack state of
its own. `NavigationHost` builds one per host — see "Mounting a host" below — and provides it as
`LocalNavigator`, scoped to that host's own content. Nothing outside a `NavigationHost` can read
it, so a screen never injects `Navigator` directly: it raises a navigation effect from its state
holder and replays it against `LocalNavigator.current` at the composable.

`NavigatorExtensions` adds naming aliases over the core operations — `pop()`/`pop(count)` for
`popBack`, `popTo(...)` for `popBackTo`, `reset(route)`/`reset(routes)` for `replaceAll` — so a
caller can use back-stack vocabulary without every implementation supplying it.

## Mounting a host

`NavigationHostParams` is controlled like a `TextField`: the caller supplies `backStack` (its own
state — a `StateFlow` collected in a ViewModel, typically) and is notified of every navigation
command through `onBackStackChange`. `NavigationHost` never owns that state itself, and carries no
deep-link concept of its own — see "Deep links" below for why that stays a single root state
holder's job instead of a `NavigationHost` parameter, since a host can be mounted anywhere a
feature needs its own local back stack (a bottom sheet's internal steps, a wizard), not just at the
app root.

`bottomSheetEntry`/`modalEntry` (`presentation.host`) register a route to render in a bottom sheet
or modal instead of the primary pane — a thin `entry` wrapper that only adds the metadata key the
host reads to pick the render surface. `transitionSpec`/`popTransitionSpec`/`predictivePopTransitionSpec`
on `NavigationHostParams` take a `NavTransitionScope<R>`/`PredictiveNavTransitionScope<R>`
(`presentation.transition` — typealiases over the Navigation3
`AnimatedContentTransitionScope<Scene<R>>` function types); leave them `null` for `impl`'s
defaults, tuned via `NavigationDefaults`.

The bottom-sheet scene is a bare bottom-aligned container on purpose: a scrim, a drag handle and
outside-tap dismiss belong to the consuming app's design system, not to a navigation library. Swap
the content and the strategy — which claims the whole run of consecutive sheet entries, so a sheet
that pushes another sheet stays one surface — keeps working unchanged.

## Guards

A `NavigationGuard` is evaluated by `Navigator` (`push`/`navigate`/`replace`/`replaceAll`, inside
`BackStackNavigator`) before a route reaches the back stack — never during composition, so a
blocked route never renders. It returns a `GuardResult` (`Allow`/`Block(reason)`/`Redirect(route)`).
A route that already sits on the stack passed a guard when it was added, so `pop`/`popBackTo` run
unguarded. Each feature contributes its guards into the graph; `wiring` collects the
`Set<NavigationGuard>` into one `NavigationGuardRunner`, which runs every guard against every route
unconditionally — it does not filter by route type itself.

Since a guard applies to a subset of routes, not all of them, mark the routes it applies to with a
dedicated marker interface named after the guard with a `-Guarded` suffix, and check that marker
first. This keeps "which routes need this guard" declared on the route itself instead of in a
separate registry that has to be kept in sync.

```kotlin
interface AuthGuarded : Route

class AuthGuardImpl(private val session: Session) : NavigationGuard {
    override fun evaluate(route: Route): GuardResult {
        if (route !is AuthGuarded) return GuardResult.Allow
        return if (session.isAuthenticated) GuardResult.Allow else GuardResult.Redirect(AuthRoute.SignIn)
    }
}
```

## Deep links

A public deep-link page is a `DeepLinkPage` enum entry; a `TypedDeepLinkHandler` derives its
`pages` set from that enum and only has to implement `resolve`. Each page has exactly one owning
handler — `impl` treats a duplicate as a construction-time error, not a last-one-wins.

```kotlin
enum class ProfileDeepLinkPage(override val page: String) : DeepLinkPage {
    View("profile"),
}

class ProfileDeepLinkHandler : TypedDeepLinkHandler<ProfileDeepLinkPage>(ProfileDeepLinkPage.entries) {
    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val page = pageOf<ProfileDeepLinkPage>(request.deepLink.page ?: return DeepLinkOutcome.NotFound)
            ?: return DeepLinkOutcome.NotFound
        val id = request.deepLink.query("id") ?: return DeepLinkOutcome.Rejected("missing id")
        return DeepLinkOutcome.Navigate(listOf(ProfileRoute.View(id)))
    }
}
```

`DeepLinkDispatcher` routes a raw link to the handler that owns its page; `DeepLinkIngress`
publishes a runtime link from cold start, a new intent, a notification tap or a `UIApplicationDelegate`
URL callback. `buildDeepLinkUri`/`DeepLinkPage.buildUri` build the outbound form of the same link.

**One rule decides what the page is.** On a custom scheme the host *is* the first page
(`navkit://booking/42` → `booking`, `42`), because there is no domain there to be a host; on
`http(s)` the host is a domain and only the path counts
(`https://example.com/booking/42` → the same). Both forms of a link therefore reach the same
handler, so a feature declares its page once and gets the app-scheme and web forms for free. The
rule is derived from the scheme rather than from a configured list of app schemes — a list is one
more thing to keep in sync with `AndroidManifest.xml` and `Info.plist`, and buys nothing.

Resolving and applying a link is the single root state holder's job, not `NavigationHost`'s — a
host can be mounted anywhere a feature needs its own local back stack, but there is only one
cold-start link stream for the whole app. The root collects `DeepLinkEvents`/`DeepLinkDispatcher`
like any other dependency and applies a resolved `DeepLinkOutcome.Navigate` through the same
`onBackStackChange` it already owns.

## Results

`NavigationResultStore` passes a value back from one screen to another without a shared state
holder. The reified `consume<T>(key)` extension wraps the `KClass<T>` overload:

```kotlin
resultStore.set("selected_photo", photoUri)
// ... later, on the screen awaiting the result
val photoUri = resultStore.consume<String>("selected_photo")
```

## Back handling

`NavigationBackHandler(enabled) { … }` (`impl`, `presentation.back`) lets a composable — a bottom
sheet, an in-screen editor — intercept back ahead of the host's own pop, for as long as it is in
the composition. It registers against the `BackDispatcher` the mounted host provides through
`LocalBackDispatcher`, which is the same object the host dispatches through. `BackDispatcher.register`
is also callable directly and returns an `AutoCloseable`.

## Navigation events

`BackStackNavigator` emits a `NavigationEvent` (`Push`/`Pop`/`Replace`/`ReplaceAll`/`Blocked`) on
every command, into an injected `NavigationEventSink`. The event type is public rather than
internal to `impl` precisely so an app can render the stream in whatever debug console it already
has. `wiring` collects `Set<NavigationEventSink>` — a debug console, an analytics tracker and a
test recorder all want the same stream, and none should have to displace the others to get it —
and an app that contributes none gets `NavigationEventSink.NoOp`.

## Feature-owned graphs

`NavigationGraphProvider` (`fun EntryProviderScope<Route>.provide()`) lets a feature register its
own entries without the composition root importing that feature's screens directly; the root
collects the contributed `Set<NavigationGraphProvider>` and calls each inside its `NavigationHost`
`entries` block.

## What changed in the port

The source module was Android-only and lived inside an application. Every difference below is one
of those two facts, not a change of design.

| | Android original | Here | Why |
|---|---|---|---|
| Result store | `consume(key, Class<T>)` | `consume(key, KClass<T>)` | `KClass.isInstance` is the one runtime type check the common stdlib offers. |
| Deep-link ingress | `publish(Intent)` on the interface | common `publish(uri)`, `publish(intent)` an `androidMain` extension | an iOS caller never sees a member it cannot satisfy. |
| Navigation logs | emitted to a global debug-console object | `NavigationEvent` + injected `NavigationEventSink` | the library cannot depend on one app's console; an injected sink is also what lets a test assert on what the navigator emitted. |
| Back handling | an injected `BackDispatcher` **and** a private global `ComposeBackDispatcher` the host actually consulted | one `BackDispatcher`, provided at the host as `LocalBackDispatcher` | with two mechanisms a caller could register with the one nothing dispatches through, and silently never fire. |
| Deep-link bridge | `object RuntimeDeepLinkBridge` | a class, bound as a singleton | two tests in one process no longer share a channel. |
| Deep-link parsing | `java.net.URI`, app scheme hardcoded | `io.ktor.http.Url`, scheme rule derived | multiplatform, and one less thing to keep in sync with the platform manifests. |
| Callback list | `CopyOnWriteArrayList` | `MutableStateFlow<List<…>>` + `update` | a compare-and-set loop is available on every platform; `dispatch` still walks a snapshot. |
| Sheet back | `androidx.activity.compose.BackHandler` | `androidx.navigationevent.compose.NavigationBackHandler` | the Compose `BackHandler` is deprecated in favour of the navigation-event API Navigation3 itself is built on. |
| Dependency scopes | `implementation` throughout | `api` for types in a module's own signatures | these are consumed as libraries, so a consumer must be able to compile against `Route : NavKey` and `ImmutableList`. |
| Detekt findings | `ignoreFailures`, cleared by a baseline-diffing pre-commit hook | findings fail the build | the port starts at zero findings, so there is no backlog a baseline has to hold back. |

The layer packages this module is organised into — `data`, `domain`, `presentation` — are checked
by the `LayerPackageRequired` and `LayerPackageBoundary` Detekt rules rather than left to review;
see the root README's "Static analysis".
