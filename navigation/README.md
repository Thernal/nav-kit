# navigation

Public Navigation3 contracts: routes, the navigator command surface, the `NavigationHost` render
contract, guards, deep links, back handling, and the two mechanisms for passing data between
screens. `impl` owns every concrete behavior; `api` never depends on it.

## Module layout

- `api` — Navigation3 routes, the navigator command surface, deep-link/guard/back contracts,
  the `NavigationHost` render contract, `NavigationResults` and `NavigationArguments`, `DeepLinkIngress`,
  the bottom-sheet route marker, and the `NavigationEvent` stream.
- `impl` — the Navigation3 host (internal), the `BackStackNavigator` command adapter it builds per
  host, overlay scenes, animations, deep-link dispatch and parsing, guards, results, arguments.
- `wiring` — binds the dispatchers, guard runner, result and argument stores and the renderer into
  an application graph, and contributes each composition local as a `ProvidedValue`.

An `impl` declaration that would collide with an `api` name carries the `Impl` suffix, composables
included: `NavigationHostImpl` is the internal render of `api`'s `NavigationHost`, the same way
`NavigationResultsImpl` implements `NavigationResults`. `Navigator` is not bound
  here — it holds no state; `NavigationHost` builds and provides one per host, over the
  caller-owned `backStack`/`onBackStackChange` pair on its `NavigationHostParams`.

## Routes

`presentation.model.Route` is the marker every route implements (`interface Route : NavKey`,
`@Immutable`). Carrying Navigation3's marker here is what keeps it out of the features: a route
is declared as `data object Home : Route`, and no feature module ever names a Navigation3 type
to describe its own destinations. Public cross-feature routes live in the owning feature's `api`; internal
wizard/tab/sheet routes stay in its `impl`. Keep payloads small identifiers, not repositories,
large models, or platform objects — routes cross module boundaries and often survive process death.

## Navigator — the command surface

`Navigator` (`push`/`pop`/`replace`/`navigate`/`popBackTo`/`popBack`) holds no back-stack state of
its own. `NavigationHost` builds one per host — see "Mounting a host" below — and provides it as
`LocalNavigator`, scoped to that host's own content. Nothing outside a `NavigationHost` can read
it, so a screen never injects `Navigator` directly: it raises a navigation effect from its state
holder and replays it against `LocalNavigator.current` at the composable.

Every command that adds routes returns a `NavigationOutcome` — `Applied`, `Rewritten(stack, reason)`
or `Deferred` — because guards make it a real question whether it happened. Before this the only
report was an application-wide event stream, so a call site could not tell "we moved" from "we were
sent somewhere else" from "nothing happened". The pops return a `Boolean`, which is the whole of the
question there.

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

`bottomSheetEntry` (`presentation.host`) registers a route to render in a bottom sheet instead of
the primary pane — a thin `entry` wrapper that only adds the metadata key the host reads to pick the
render surface. An app that wants another surface writes its own `SceneStrategy` and hands it over
through `NavigationHostParams.sceneStrategies`, which the host consults before its own.
`transitionSpec`/`popTransitionSpec`/`predictivePopTransitionSpec`
on `NavigationHostParams` take a `NavTransitionScope<R>`/`PredictiveNavTransitionScope<R>`
(`presentation.transition` — typealiases over the Navigation3
`AnimatedContentTransitionScope<Scene<R>>` function types); leave them `null` for `impl`'s
defaults, tuned via `NavigationDefaults`.

The bottom-sheet scene is a bare bottom-aligned container on purpose: a scrim, a drag handle and
outside-tap dismiss belong to the consuming app's design system, not to a navigation library. Swap
the content and the strategy — which claims the whole run of consecutive sheet entries, so a sheet
that pushes another sheet stays one surface — keeps working unchanged.

## Guards

A `NavigationGuard` decides which back **stacks** may exist, not which routes may be pushed:

```kotlin
fun interface NavigationGuard {
    fun evaluate(old: ImmutableList<Route>, new: ImmutableList<Route>): GuardVerdict
}
```

`old` is the stack as it stands, `new` the one being proposed, and every decision is the stack it
returns — allow is `Resolved(new)`, refuse is `Resolved(old, reason)`, and anything else is a
rewrite. Seeing the transition rather than a destination is what lets one contract express a
redirect that keeps the original intent (`Resolved((old + SignIn(next = new.last()))
.toImmutableList())`) and a refusal to *leave* a screen with unsaved work, which a
destination-only guard cannot say at all.

It is evaluated in exactly two places, which between them are every way a stack can change:

- `BackStackNavigator.mutate`, the single primitive `push`/`navigate`/`replace`/`replaceAll`/
  `popBack`/`popBackTo` and a caller's own `buildStack` all run through, before anything is
  written back;
- the host, on whatever it was handed directly — a deep link the root applied through
  `onBackStackChange`, or a stack restored after process death. A pure derivation, so `NavDisplay`
  is never given the unresolved stack and a refused route never renders; the correction reaches the
  caller's own state one frame later.

`onBackStackChange` must therefore be a plain setter. The host writes its correction back through
it, so a callback that filters what it is given, or re-applies a pending link, will overwrite that
correction and be corrected again. Owning the state is the caller's job; editing it on the way in is
not.

Guards are folded in contribution order, each seeing the previous one's output as `new`, and the
fold repeats until the stack stops changing — so a route a guard *introduced* is guarded like any
other, including by the guard that introduced it. `evaluate` must therefore be pure and cheap. A
guard may drop and insert routes but may not reorder what it keeps or empty the stack;
`NavigationGuardRunnerImpl` rejects a verdict that does, loudly, because a malformed stack corrupts
navigation for every feature.

Each feature contributes its guards into the graph and `wiring` collects the `Set<NavigationGuard>`
into one application-wide `NavigationGuardRunner`. A single host can add its own on top, through
`NavigationHostParams.guards` — a wizard's internal rules, or a guard whose dependencies live in a
feature scope and so could never reach the app graph's multibinding. The caller already holds that
scope and hands the guard over the same way it hands over `decorators`; `extendedWith` returns the
app-wide runner unchanged when there are none.

### Staying correct after the answer changes

A guard whose answer depends on something that moves — a session, a role, a flag — overrides
`invalidations`:

```kotlin
override val invalidations: Flow<Unit> = session.state.drop(1).map { }
```

Every mounted host collects the merged stream and revalidates its own stack on each emission,
`resolve(old = current, new = current)`. That is what makes a route which has *become* invalid leave
the stack, rather than sitting there until something else happens to navigate. Collection is scoped
to the host's composition, so an unmounted host costs nothing — and because every mounted host
collects, make the flow hot; a cold flow that does work per collector does it once per host.

Revalidation runs the same synchronous `resolve` as everything else, which is what keeps it cheap
and is why an asynchronous verdict will never be reachable from it.

### Writing one

Almost every guard is about the routes of one type, and extends `RouteGuard` rather than
implementing the interface directly. Mark those routes with an interface named after the guard —
`AuthGuard` guards routes marked `AuthGuarded` — and pass the narrowing, so the pairing is checked
by the compiler instead of by a convention each implementation has to remember:

```kotlin
interface AuthGuarded : Route

class AuthGuardImpl(private val session: Session) : RouteGuard<AuthGuarded>({ it as? AuthGuarded }) {
    override val reason = SignInRequired

    override fun redirect(route: AuthGuarded, stack: ImmutableList<Route>): Route? {
        if (session.isAuthenticated) {
            return null
        }
        return AuthRoute.SignIn
    }
}
```

`final override` on `RouteGuard.evaluate` is the point: a subclass cannot skip the narrowing and
silently apply itself to every route in the application.

`RouteGuard` judges **every** matching route in the proposed stack, not only those entering, and
answers by substitution rather than by handing back the previous stack. Both follow from the same
requirement — "`Secret` requires a session" holds for any stack containing `Secret`, however it got
there — and both are what make it correct when a stack is revalidated in place
(`resolve(old = current, new = current)`) and there is no transition to reason about. A rule that
genuinely is about the transition implements `NavigationGuard` directly, so it can compare `old`
with `new` — and keeps itself narrow. `old` does not move during the fold, so a rule broad enough to
reject *any* difference between the two undoes every rewriting guard, which rewrites again, and the
pair fails at the round limit instead of settling. Phrase it about one screen's own routes ("`Edit`
may not leave the stack"), never about movement in general.

### Deciding later

`GuardVerdict.Deferred(meanwhile, resolve)` is the answer for a guard that cannot decide yet — a
token to refresh, a confirmation to collect, a server to ask:

```kotlin
GuardVerdict.Deferred(meanwhile = old) { navigator ->
    navigator.push(SignIn)
    val signedIn = results.await<Boolean>(SIGN_IN_RESULT)
    if (signedIn) GuardVerdict.Resolved(new) else GuardVerdict.Resolved(old, SignInCancelled)
}
```

`meanwhile` is what exists until it settles: `old` holds the navigation without showing anything
new, `old + Loading` shows a placeholder. Returning `Resolved(new)` at the end continues to the
route the user originally asked for — which is the point of deferring rather than redirecting, and
the thing a redirect cannot express because it loses the original intent.

Three rules follow, and all three are load-bearing:

- **Only the mounted host awaits a deferral.** It owns a scope tied to its own composition, so an
  unmounted host cancels what it started, and there is one driver however many ways the stack can
  change. The navigator and composition never start work: a deferred command writes `meanwhile`
  and hands the deferral to its host, and the host waits on one deferral at a time, keyed on the
  stack that was attempted. So a revalidation storm or a double tap cannot launch the same one
  twice. The key is deliberately not the verdict: the placeholder push changes the verdict, and a
  wait keyed on it cancelled itself the moment it showed its prompt.
- **A deferral is abandoned when the stack moves without it.** A command that changes the stack —
  backing out of the placeholder — or a stack the host did not write — a deep link, a tab bar
  handing over another list — ends the wait, and its answer is never applied. The `Navigator`
  passed to `resolve` is exempt: its pushes are part of the wait, and a deferral it meets is left
  alone rather than started.
- **A guard that defers must answer synchronously once its deferral has settled**, from a cached
  result. It is asked again as soon as the settled stack is applied; a guard that defers a second
  time for the same stack never converges, and the host leaves it alone rather than spinning.

A placeholder shown as `meanwhile` should implement `TransientRoute`. Routes survive process death
and an in-flight coroutine does not, so a restored `Loading` would be a screen with nothing left to
resolve it; the host drops transient routes from a restored stack before anything is rendered or
guarded.

`BlockReason` is an empty-vocabulary interface the application fills in: a navigation layer cannot
know whether a refusal reads as a sign-in prompt, a paywall or a permission error, so it only
renders `message` into `NavigationEvent.Blocked`.

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
`onBackStackChange` it already owns. That path does not go through `Navigator`, which is why the
host resolves what it is handed: a link is the one navigation input that comes from outside the
app, and it is guarded without the root having to remember to ask.

## Passing data between screens

Two mechanisms, told apart by direction. Using one for the other is the mistake they exist to
prevent.

| | Result | Argument |
|---|---|---|
| Direction | a closing screen → a screen already in the stack | an opening screen → screens about to be pushed |
| Who waits | the producer is gone, the consumer stays | the consumer does not exist yet |
| Lifetime | one delivery, gone once consumed | as long as the screens that need it |
| Surface | `NavigationResults` | `NavigationArguments` |

Both are application-scoped objects reached through a composition local, contributed by `wiring`
into the graph's `Set<ProvidedValue<*>>` and installed once at the composition root — the same
route `LocalNavigationHostRenderer` takes. **Both are in memory only**: a value is lost on process
death while the routes that would have read it are restored, so a consumer that finds nothing
treats it as a first visit or restarts its flow, never as an error.

### Results — backwards

A key is declared once, next to the routes of the feature that produces the result, and used by
both sides. That is what makes a producer and a consumer disagreeing about the type a compile
error rather than a `null` nobody notices:

```kotlin
val SelectedPhoto = resultKey<String>("selected_photo")

// the closing screen
screenResults.post(SelectedPhoto, photoUri)

// the screen it returns to, from its own composable
ResultEffect(SelectedPhoto) { photoUri -> state.onPhotoSelected(photoUri) }
```

`ResultEffect` collects `pending` and consumes inside a `LaunchedEffect`. Navigation3 composes only
the entries of the current scene, so a covered screen is not composed and the effect does not run —
it runs when the user comes back to it, which is exactly when a returning result is wanted. The
composable forwards to its state holder; it does not decide.

`pending` exposes names, not values, so one feature's results are not readable by every screen.
Consuming removes the value: one delivery, never two. A value posted under a name that another
feature already declared with a different type throws rather than reading `null`.

### Arguments — forwards

A value set on one screen and read by the next several, without being threaded through every route
in between. Its lifetime is **derived from the back stack**, never counted:

```kotlin
val CheckoutDraft = argumentKey<Draft>("checkout_draft")

screenArguments.put(
    key = CheckoutDraft,
    value = draft,
    scope = whileInStack { route -> route is CheckoutRoute },
)
navigator.push(CheckoutAmount)
```

A consumer reference count released on `DisposableEffect` was the obvious design and it does not
work: Navigation3 composes only the top entry, so a screen that pushes the next one leaves
composition while it is still in the stack and the count reaches zero one push early. The stack
already states who is present, so the stack is what decides. Scoping to a sealed flow type rather
than to one screen is also what makes back, `popBackTo`, a guard rewrite and a deep link all come
out right without any of them being handled separately.

The mounted host applies this through `ArgumentPruner.pruneFor(stack)` after every stack change.
That is a second interface on the same object, injected into the host rather than exposed on
`NavigationArguments`, so `pruneFor` is not reachable from the composition local every screen can read.
Two rules follow from where it runs:

- **Only the outermost host prunes.** The store is application-scoped while a stack is per host, so
  a nested host pruning against its own stack would delete the arguments of the flow that mounted
  it.
- **Never prune from a guard.** `NavigationGuard.evaluate` must stay pure and runs several times
  per navigation.

Put an argument in the same action that pushes the routes which read it. An argument that has never
been alive in the stack is kept until it is, and dropped the first time it is alive and then is not
— so the stack change that starts a flow cannot delete the value that flow was started with. A put
whose scope is already alive in the last pruned stack counts as alive from the start: a flow that
updates its own argument on every screen still loses it when the user backs out of the flow.

For anything that must survive process death, or anything larger than a small serializable value,
put it in a repository and keep only its id here. A flow mounted as a nested host has a third
option: the flow's root entry owns a `ViewModel`, and its `SavedStateHandle` is cleared by
Navigation3 when that entry is popped.

## Back handling

`NavigationBackHandler(enabled) { … }` (`impl`, `presentation.back`) lets a composable — a bottom
sheet, an in-screen editor — intercept back ahead of the host's own pop, for as long as it is in
the composition. It registers against the `BackDispatcher` the mounted host provides through
`LocalBackDispatcher`, which is the same object the host dispatches through. `BackDispatcher.register`
is also callable directly and returns an `AutoCloseable`.

**`Navigator.popBack()` consults that same dispatcher**, so an interceptor is heard whether back
came from the system gesture or from a button in a screen calling `popBack()` itself. Only the
gesture used to ask, which made "discard unsaved changes?" work in one of the two and silently not
in the other — the same class of split the port table below records having removed once already.
`popBackTo` deliberately does not consult it: that is a jump, not a back. A callback that lets back
through by calling `popBack()` from inside its own handler is not dispatched to again, because a
nested dispatch consumes nothing.

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
| Result store | `NavigationResultStore`, `consume(key, Class<T>)` | `NavigationResults`, a declared `ResultKey<T>` | the original asserted the type where the value was read, so a producer writing an `Int` and a consumer asking for a `String` got `null` silently. |
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
