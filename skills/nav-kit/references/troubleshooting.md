# Troubleshooting and review

## Contents

1. By error message
2. By symptom
3. Review checklist

## 1. By error message

| Message | Cause | Fix |
|---|---|---|
| `IllegalStateException: Unknown screen <route>` | a route on a host's stack has no entry in that host. Usual: a guard substituted a sign-in route or pushed a placeholder into a **nested** host; a deep link returned a route the root does not register | register the route in that host (`navEntry<…>`) |
| `` An `entry` with the same `clazz` has already been added: X `` | one route class registered twice in one host — often two `NavigationGraphProvider`s, or a provider plus an inline entry | keep one registration per host |
| `NavDisplay backstack cannot be empty` | a host received an empty stack: initial state empty, an owner setting `emptyList()`, `popBack(force = true)` on one route, a deep link applied with no routes | never hand a host an empty list; `replaceAll` requires a non-empty list |
| `Navigation back stack cannot be empty` | `replaceAll(emptyList())` | pass at least one route |
| `Navigation guards did not settle after 8 rounds; two of … rewrite each other's stack.` | two guards undo each other, typically a transition guard rejecting *any* change between `old` and `new` against a rewriting `RouteGuard` | phrase the transition rule about one screen's own routes; make redirects stable (the substitute must not itself be guarded into something else) |
| `X emptied the back stack; a guard may refuse a stack, never leave none.` | a verdict (or a deferral's `meanwhile`) removed every route | fall back to `old`, or keep the root route |
| `X reordered the routes it kept; a guard may drop and insert, not reorder.` | a guard rebuilt the stack in another order | substitute in place; insert, drop — do not sort or move |
| `X returned a stack with a duplicate route: …` | a rewrite put the same route on the stack twice | de-duplicate, or use `RouteGuard` (it de-duplicates) |
| `Deep link page 'p' is claimed by A and B` | two handlers list the same page | one owner per page |
| `Deep link page cannot be blank` | a handler's `pages` contains `""` | fix the page set |
| `Deep link handlers are registered but no DeepLinkBase is, so every link would be NotFound` | handlers contributed, no `DeepLinkBase` | contribute a `DeepLinkBase` per scheme/domain |
| `A deep link base is scheme://host/path with no query or fragment, got '…'` / `A web deep link base needs a host…` / `A deep link base without a host cannot have a path…` | malformed `DeepLinkBase("…")` | `myapp://`, `https://example.com`, `https://example.com/app` |
| `A deep link page cannot be blank` | `buildDeepLinkUri` with a blank page | pass the page name |
| `` Result `n` was posted as A and read as B. `` / `` Argument `n` was put as A and read as B. `` | two keys share a name with different types | prefix key names with the feature; declare each key once |
| `A result key needs a name` / `An argument key needs a name` | blank key name | name it |
| `count must be non-negative` | `popBack(-1)` | — |

## 2. By symptom

| Symptom | Likely cause | Fix |
|---|---|---|
| a host draws nothing | the root did not install `LocalNavigationHostRenderer` (graph's `providedValues`), or running in a preview | `CompositionLocalProvider(values = graph.providedValues.toTypedArray())` at the root |
| every command returns `Rewritten` with an empty stack, nothing happens | the composable is outside any `NavigationHost` (the default no-op navigator) | call from inside a host's entry, or pass the right navigator down |
| `push` lands in the wrong stack | inside a nested host, `LocalNavigator` is the nested one | read `LocalNavigator.current` outside the nested `NavigationHost` and pass it down |
| a guarded screen stays visible after sign-out | the guard has no `invalidations`, or it is cold/never emits, or it emits only on collect | `invalidations = state.drop(1).map { }` from a hot `StateFlow` |
| the host keeps "fighting" the owner; stack flickers or corrections vanish | `onBackStackChange` filters, transforms or re-applies a pending link | make it a plain setter; move rules into guards |
| a guard's decision is not applied / a deferred prompt never resolves | the guard defers again after settling; or the wait was abandoned (user left the placeholder, a deep link or another list arrived); or the host was unmounted | update the cached answer before returning from `resolve`; re-trigger the navigation |
| a PIN/loading placeholder is stuck after restoring | the placeholder is not a `TransientRoute` | implement `TransientRoute` |
| a result never arrives | wrong key object/name; consumer not composed (covered, or in a hidden tab); `ResultEffect` placed in a composable that is not the waiting screen; value consumed elsewhere | declare one shared key; put `ResultEffect` in the waiting screen |
| a result arrives but the UI forgets it | consumer state in `remember` | `rememberSaveable` or an entry-scoped ViewModel |
| an argument is `null` on a later step | the scope names one route and that route left the stack (a `replace`, a guard rewrite); it names routes that exist only inside a nested host; a different key object with another name; process death | scope to the flow's sealed type in the outermost stack, put in the action that starts the flow; treat `null` as "restart the flow" |
| an argument outlives its flow | its scope was never alive (flow never started), or it is scoped to nested-host routes | `remove` it; scope to outermost-stack routes |
| typing in a field is lost or the cursor jumps | field bound to the argument store (not snapshot state) or to a `StateFlow` collected a frame late | own the field in `rememberSaveable`/snapshot state and write through |
| "discard changes?" not shown for some exits | `NavigationBackHandler` only sees `popBack` while composed | add a transition guard for jumps, `replaceAll` and links |
| the confirm dialog reappears after "Discard" | leaving with `popBack()` while the handler is still enabled | leave with `popBackTo(inclusive = true) { it is ThisRoute }` |
| a back handler inside a tab also blocks the outer host's back | one `BackDispatcher` for all hosts | disable the handler when its host is not the one being navigated |
| a deep link opens but back exits the app | the handler returned only the destination | return a stack with the app's root at the bottom |
| a deep link is applied again after rotation | the activity publishes its launch intent on every `onCreate` | publish only when `savedInstanceState == null`, plus `onNewIntent` |
| a warm link opens a second copy of the app UI | activity not `singleTop` | `android:launchMode="singleTop"` and `onNewIntent` |
| iOS app closes at launch before drawing | `Info.plist` lacks `CADisableMinimumFrameDurationOnPhone` = `true` | add it |
| sessions, results or arguments reset after rotation while the screen stays | the graph is created inside the composition | one graph per process (Application / top-level `lazy`) |
| a hidden tab lost its scroll or form state | per-tab stacks over one host clear hidden entries' state | keep it in the tab-owning ViewModel |
| every link, or every link of one scheme/domain, is `NotFound` | that scheme or domain is declared on the platform but not registered as a `DeepLinkBase`, or registered with a different path | register the exact base; keep manifest, `Info.plist` and bases in step |
| a web link resolves to the page `app` (or another path prefix) | the base registered is the bare origin, not `https://example.com/app` | register the base with its path |
| a link built in the app does not open | built by string concatenation, or on a base that is not registered | `buildDeepLinkUri(base, page)` on a registered base |

## 3. Review checklist

**Routes and hosts**

- [ ] Routes implement `Route`, are `data object`/`data class`, carry ids only, and sealed per flow.
- [ ] Each host's stack is owned outside it (ViewModel), `onBackStackChange` is a plain setter, the initial
      stack is non-empty.
- [ ] Every route reachable in each host — including guard substitutes, deferral placeholders and deep-link
      stacks — has exactly one entry in that host.
- [ ] Nested hosts register the substitutes of application-wide guards.

**Commands**

- [ ] Screens use `LocalNavigator.current`; no navigator is stored in a ViewModel, singleton or DI graph.
- [ ] `navigate(route)` where the destination may already be on the stack; no duplicate routes.
- [ ] Outcomes are read where a guard can change the answer, and recorded somewhere that outlives the screen
      when shown later.

**Guards**

- [ ] Access rules are guards, not checks before `push`.
- [ ] `RouteGuard` for destinations (marker named after the guard); `NavigationGuard` only for transitions,
      phrased about one screen's routes.
- [ ] `evaluate` is pure, cheap, without I/O or side effects.
- [ ] Guards whose answer changes expose hot `invalidations` (`drop(1)` on a `StateFlow`).
- [ ] A deferring guard answers synchronously once settled; its placeholder is a registered `TransientRoute`;
      `meanwhile` is never empty.
- [ ] State guards read is application-scoped, not composition state.

**Data**

- [ ] Results for backwards, arguments for forwards; keys declared once next to the producer, names prefixed.
- [ ] Result consumers keep state in `rememberSaveable` or an entry ViewModel.
- [ ] Arguments put in the same action that starts the flow, scoped to the flow's sealed type in the
      outermost stack; fields own their state and write through.
- [ ] Nothing that must survive process death lives only in a result or an argument.

**Back and links**

- [ ] "Confirm on back" is `NavigationBackHandler`; "never leave" is a transition guard.
- [ ] Deep links resolve at the root, return whole stacks, do not check access, respect `source`.
- [ ] Every platform-declared scheme and domain is a registered `DeepLinkBase`; outbound links use
      `buildDeepLinkUri` on those bases.
- [ ] Android: `singleTop`, intent filter, publish in `onCreate` (first creation) and `onNewIntent`.
- [ ] iOS: URL type, `onOpenURL` → ingress, `CADisableMinimumFrameDurationOnPhone`.

**Wiring**

- [ ] One DI graph per process; root installs `providedValues` once.
- [ ] Guards, handlers and sinks contributed `@IntoSet` (or passed to the `*Impl` constructors).
