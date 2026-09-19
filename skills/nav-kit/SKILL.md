---
name: nav-kit
description: Builds, wires, reviews and debugs navigation in Compose Multiplatform apps that use nav-kit, the Navigation3 kit in packages io.thernal.navkit.navigation.* (Route, NavigationHost, NavigationHostParams, LocalNavigator, NavigationOutcome, RouteGuard, NavigationGuard, GuardVerdict, resultKey, ResultEffect, argumentKey, whileInStack, DeepLinkHandler, DeepLinkIngress, NavigationBackHandler, bottomSheetEntry, NavigationGraphProvider, NavigationWiring). Use it for any navigation work in such a project, even when nav-kit is not named - installing the kit, adding screens or routes, navigating, sign-in redirects and other guards, unsaved-changes prompts, 401 or PIN re-authentication, returning a result, sharing a draft across a multi-step flow, deep links and notification taps, tabs or nested flows, bottom sheets, transitions, navigation analytics, tests - and for crashes such as "No entry is registered for <route>" or guards that do not settle. Not for Jetpack NavController/Navigation2 code or plain Navigation3 without nav-kit.
---

# nav-kit

nav-kit is a navigation layer over Navigation3 for Compose Multiplatform (Android, iosArm64,
iosSimulatorArm64): routes as values, a controlled host, a stateless navigator whose commands report
what happened, stack-level guards, typed results and arguments, back interception, and deep links.
Source and the complete API guide: https://github.com/Thernal/nav-kit — `navigation/api/README.md`;
`sample/` runs every capability.

Three modules. `navigation/api` (package `io.thernal.navkit.navigation.api`) holds the contracts and is
all a feature module needs. `navigation/impl` implements them. `navigation/wiring` binds them into a
[Metro](https://github.com/ZacSweers/metro) graph.

## 1. Orient before writing

Most nav-kit bugs come from adding a second mechanism beside one the project already has, or from
missing a registration. Find the existing pieces first:

```sh
grep -rn --include=*.kt -e "NavigationWiring" -e "NavigationHostRendererImpl" -e "LocalNavigationHostRenderer provides" .  # installed?
grep -rn --include=*.kt "NavigationHostParams(" .                            # every host, and who owns each stack
grep -rn --include=*.kt -e "NavigationGraphProvider" -e "navEntry<" -e "bottomSheetEntry<" .  # where screens are registered
grep -rn --include=*.kt -e "RouteGuard<" -e ": NavigationGuard" -e "TransientRoute" .        # guards and their markers
grep -rn --include=*.kt -e "resultKey<" -e "argumentKey<" .                  # data keys (names already taken)
grep -rn --include=*.kt -e "DeepLinkHandler" -e "DeepLinkPage" -e "DeepLinkBase(" .  # pages claimed, schemes and domains registered
```

If nothing is installed, read [references/setup.md](references/setup.md) before anything else.

## 2. The model

These hold everywhere; each reference builds on them.

1. **A route is a small immutable value** implementing `Route` (a Navigation3 `NavKey`):
   `data object Home : Route`, `data class Order(val id: String) : Route`. Ids only — routes cross
   modules and may be persisted by their owner.
2. **A host never owns its stack.** `NavigationHost(NavigationHostParams(backStack, onBackStackChange))`
   is controlled like a `TextField`. The owner (a ViewModel) holds `StateFlow<ImmutableList<Route>>`, and
   `onBackStackChange` is a plain setter, because the host writes guard corrections back through it — a
   setter that filters or re-applies overwrites them.
3. **Screens navigate through `LocalNavigator.current`**, the navigator of the nearest host. It is built
   per host and never injected or stored; a ViewModel emits navigation effects that the composable
   replays against it.
4. **Commands that add routes return `NavigationOutcome`** (`Applied`, `Rewritten(stack, reason)`,
   `Deferred(stack)`); pops return `Boolean`. Guards can refuse, redirect or defer any command, so the
   outcome is the only truthful answer.
5. **Access rules are guards, not call-site checks.** Guards judge whole stacks for every command, for
   every stack handed to a host directly (deep links, restored state) and on invalidation. A check before
   `push` is bypassed by a deep link or `replaceAll`.
6. **Results travel backwards, arguments forwards.** `resultKey` + `post` + `ResultEffect` for a value
   handed to a screen already on the stack; `argumentKey` + `put(…, whileInStack { … })` for a value read
   by screens about to open. Both are in memory only.
7. **Deep links are resolved once, at the root**, by the state holder that owns the root stack. The
   application registers a `DeepLinkBase` per scheme and domain; the parser strips the matching base, a
   link matching none is `NotFound`. A handler returns a whole stack and does not check access — the
   host guards what it is handed.
8. **Application services arrive as composition locals** installed once at the root from the graph:
   `LocalNavigationHostRenderer`, `LocalNavigationResults`, `LocalNavigationArguments`. Without the
   renderer a host draws nothing.

## 3. Route the task

| The task | Mechanism | Read |
|---|---|---|
| install the kit, write the composition root, platform deep-link entry points | graph + root + root ViewModel | [setup.md](references/setup.md) |
| add a screen or a flow, navigate, read outcomes, navigate from a ViewModel | routes, entries, `Navigator` | [screens-and-navigation.md](references/screens-and-navigation.md) |
| tabs, a wizard with its own stack, bottom sheets, animations | nested host, `bottomSheetEntry`, `NavAnimations` | [screens-and-navigation.md](references/screens-and-navigation.md) |
| members-only screens, sign-in redirect, remove a screen on sign-out, feature flags | `RouteGuard` + marker + `invalidations` | [guards.md](references/guards.md) |
| 401 / PIN / token refresh before continuing | `GuardVerdict.Deferred` + `TransientRoute` | [guards.md](references/guards.md) |
| "discard changes?" on back | `NavigationBackHandler` | [back-handling.md](references/back-handling.md) |
| forbid leaving a screen by any route | transition `NavigationGuard` | [guards.md](references/guards.md), [back-handling.md](references/back-handling.md) |
| return a value; share a draft across steps | results; arguments | [passing-data.md](references/passing-data.md) |
| open the app from a link or notification; build a link | `DeepLinkHandler`, `DeepLinkIngress` | [deep-links.md](references/deep-links.md) |
| analytics or logging of navigation; unit tests | `NavigationEventSink`; the `*Impl` classes | [events-and-testing.md](references/events-and-testing.md) |
| a crash, an exception message, a screen that does not appear, a value that is null; reviewing navigation code | — | [troubleshooting.md](references/troubleshooting.md) |

Read only the references the task needs.

## 4. Implement, register, verify

A nav-kit change is usually correct in its own file and broken by a missing registration somewhere else.
Before calling the work done, walk this:

- [ ] Every route that can appear in a host's stack has **one** entry in **that** host — including routes a
      guard substitutes (sign-in) or a deferral shows (a placeholder), and including nested hosts.
- [ ] New guards, deep-link handlers and event sinks are contributed `@IntoSet` (or passed to
      `NavigationGuardRunnerImpl` / `DeepLinkDispatcherImpl` / the sink fan-out when wired by hand).
- [ ] Every scheme and domain the platform delivers (intent filters, `CFBundleURLTypes`, verified
      domains) is registered as a `DeepLinkBase`, and outbound links are built on those bases.
- [ ] New result and argument keys are declared once, next to the producing feature's routes, with a
      feature-prefixed name.
- [ ] No host can be handed an empty stack.
- [ ] The project builds, and its tests pass. For a guard, add a `commonTest` using
      `NavigationGuardRunnerImpl` (see [events-and-testing.md](references/events-and-testing.md)).
- [ ] The review checklist in [troubleshooting.md](references/troubleshooting.md) passes.

## 5. Traps that look reasonable

| Tempting | Why it breaks | Instead |
|---|---|---|
| `if (signedIn) navigator.push(Account)` | deep links, `replaceAll` and restored stacks skip the check | `RouteGuard` over an `AuthGuarded` marker |
| storing `LocalNavigator.current` in a ViewModel | it belongs to one host and one composition | emit an effect; replay it in the composable |
| filtering in `onBackStackChange` | overwrites the host's guard corrections, which are then re-applied | a plain setter; rules go in guards |
| `remember` for what a result updates | the waiting screen leaves composition while covered | `rememberSaveable` or an entry-scoped ViewModel |
| a `TextField` bound to `arguments.get(key)` | the store is not snapshot state; typing is lost | own the field state, write each change through |
| an argument scoped to one screen | dies when that route leaves the stack (a `replace`, a guard rewrite) while other steps still read it | scope to the flow's sealed type |
| `NavigationBackHandler` to "block leaving" | only sees `popBack` while composed | a transition guard |
| a deep-link handler checking the session | duplicated rule; the host guards the stack anyway | return the stack; let guards act |
| a new scheme or domain added only to the manifest / `Info.plist` | its links start with no registered base: `NotFound` | also contribute a `DeepLinkBase` |
| a link string concatenated by hand | drifts from what the parser reads | `buildDeepLinkUri(base, page, query)` on a registered base |
| a guard with side effects or I/O in `evaluate` | runs many times per navigation | pure `evaluate`; async work via `Deferred` |
| creating the DI graph inside a composable | rebuilt on activity recreation; stores reset while the stack survives | one graph per process |
