# Navigation events and tests

## Navigation events

Every navigator command emits a `NavigationEvent` (`io.thernal.navkit.navigation.api.presentation.log`) into
the app's `NavigationEventSink`, synchronously on the command's call path.

| Event | When |
|---|---|
| `Push(previous, pushed)` | `push`; a `navigate` that grew the stack |
| `Pop(popped, backTo)` | `popBack` removed a route |
| `Replace(old, new)` | `replace` |
| `ReplaceAll(routes)` | `replaceAll`; `buildStack`, `popBackTo` or a shrinking `navigate` that changed the stack |
| `Blocked(attempted, applied, reason)` | a guard rewrote the command — instead of the command's own event |
| `Deferred(attempted, meanwhile)` | a guard deferred |

Stacks set directly (deep links applied by the root, revalidation after an invalidation) emit nothing. Each
event has `message` (one line) and `detail` (multi-line) for debug consoles.

### Recipe: screen analytics

```kotlin
class ScreenTracker(private val analytics: Analytics) : NavigationEventSink {
    override fun emit(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.Push -> analytics.screen(event.pushed::class.simpleName.orEmpty())
            is NavigationEvent.Blocked -> analytics.event("navigation_blocked", event.reason?.message.orEmpty())
            else -> Unit
        }
    }
}

@Provides
@IntoSet
fun provideScreenTracker(analytics: Analytics): NavigationEventSink {
    return ScreenTracker(analytics)
}
```

Every contributed sink receives every event; with none, the app gets `NavigationEventSink.NoOp`. Keep `emit`
fast and non-throwing. A `Push` of a route class name is not a screen view for pops or deep links — track
`Pop`/`ReplaceAll` too, or observe the root stack's `StateFlow` if every stack change must count.

## Tests

Everything except the composable host is plain Kotlin — put tests in `commonTest` so they run on JVM and iOS.
Dependencies: `kotlin-test`, `kotlinx-coroutines-test`, and `:navigation:impl`.

| Subject | Build | Assert |
|---|---|---|
| a guard | `NavigationGuardRunnerImpl(listOf(guard))` | `resolve(old, new).stack` / `.reason`; `resolveDeferrable` is `GuardVerdict.Deferred` |
| sign-out removal | same runner | `resolve(old = current, new = current)` |
| command sequences with guards | `BackStackNavigator` over a local list (below) | returned outcomes, the list, recorded events |
| back interception | `BackDispatcherImpl()` + `BackStackNavigator` | `popBack()` returns `true` and the list is unchanged |
| results | `NavigationResultsImpl()` | `consume` once, then `null`; `pending` names |
| arguments | `NavigationArgumentsImpl()` + `pruneFor(stack)` per stack change | `get` before and after the flow leaves |
| deep links | `DeepLinkDispatcherImpl(handlers = setOf(handler), bases = setOf(base))` in `runTest` | the `DeepLinkOutcome`, including `NotFound` for an unregistered domain |
| parsing and building | `parseDeepLink(raw, bases)`; `buildDeepLinkUri(base, page)` | `page`, `pathSegments`, `query`, `base`; built links read back as the same page |

```kotlin
private class Harness(initial: List<Route>, guards: List<NavigationGuard> = emptyList()) {
    var stack: List<Route> = initial
        private set
    val events = mutableListOf<NavigationEvent>()
    val backDispatcher = BackDispatcherImpl()
    private val runner = NavigationGuardRunnerImpl(guards)

    val navigator: Navigator = BackStackNavigator(
        buildBackStack = { builder -> stack = stack.toMutableList().apply(builder) },
        resolveCanPop = { stack.size > 1 },
        resolveGuardRunner = { runner },
        backDispatcher = backDispatcher,
        events = NavigationEventSink { event -> events += event },
    )
}

@Test
fun unsavedEditorRefusesAJumpHome() {
    val drafts = ArticleDraftStore().apply { edit("changed") }
    val harness = Harness(listOf(HomeRoute, ArticleEditorRoute), guards = listOf(UnsavedWorkGuard(drafts)))

    assertFalse(harness.navigator.popBackTo { it is HomeRoute })
    assertEquals(listOf(HomeRoute, ArticleEditorRoute), harness.stack)
    assertTrue(harness.events.last() is NavigationEvent.Blocked)
}
```

A deferred guard: `(runner.resolveDeferrable(old, new) as GuardVerdict.Deferred).resolve(harness.navigator)`
inside `runTest`, completing whatever the lambda awaits from the test.

What unit tests cannot cover — verify in the running app: that every route has an entry in each host that can
show it, that the root installs the graph's locals, deep links from the platform (cold, warm, after rotation),
and state after process death (Android: put the app in the background, then `adb shell am kill <package>`;
"Don't keep activities" destroys activities but keeps the process and its graph).

The kit's own tests are a template: `navigation/impl/src/commonTest` in https://github.com/Thernal/nav-kit.
