# sample

A runnable demonstration of everything in `navigation/`, on Android and iOS, from one shared
composition. Each capability has a simple example and a real-life one; the simple one is the
smallest thing that works, the real-life one is the shape the problem actually takes in an
application.

## Running it

```sh
./gradlew :sample:app:installDebug          # Android
open sample/iosApp/iosApp.xcodeproj         # iOS, then run the iosApp scheme
```

The Xcode project builds the shared framework itself through a run-script phase, so there is no
separate Gradle step. It sets `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` because the kit
targets `iosArm64` and `iosSimulatorArm64` only — the two every Navigation3 artifact in the catalog
publishes.

## Layout

| Module | What it is |
|---|---|
| `shared` | Every screen, route, guard and binding. Android and iOS run this unchanged. |
| `app` | An Android application with one activity and one `setContent`. |
| `iosApp` | A SwiftUI shell whose only view is the shared composition. |

## The three things the composition root does

`SampleApp` is the whole of the integration, and it names no feature:

- **Composition locals arrive in one set.** `wiring` contributes each as a `ProvidedValue<*>`, and
  the root installs them with a single spread. A screen reaches the navigator, the results mailbox
  or the argument store without importing the navigation module, and adding a capability to the kit
  does not change this file.
- **The back stack is owned outside the host.** `RootViewModel` holds it. The host renders a stack
  and reports every change; it never owns one.
- **Screens arrive from the graph too.** Each example contributes a `NavigationGraphProvider`, so
  the root registers entries it has never heard of. The catalog index works the same way: examples
  contribute themselves to a `Set<SampleExample>`.

## The examples

| Group | Simple | Real life |
|---|---|---|
| Navigation | push and pop | `navigate` with a predicate, `replaceAll`, `popBackTo`, and reading the `NavigationOutcome` |
| Results | a picker posts one value back | a three-step flow returns a decision to the screen that launched it |
| Arguments | one value across two screens | a checkout draft read and updated on four screens, dropped when the flow leaves |
| Back handling | a screen intercepts back while composed | a transition guard refuses every way out, gesture or not |
| Guards | a destination rule, plus removal when the session ends | a 401 defers the decision, asks for a PIN, then continues where you were going |
| Nested navigation | one host, tabs as its stack | a stack per tab, preserved across switches, with one guarded tab |
| Deep links | one link, one route; app scheme and web form agree | a link three screens deep that reads its source and meets a guard |

### What each one is really showing

- **Navigation.** Commands answer with a `NavigationOutcome` instead of `Unit`, because a guard can
  refuse, redirect or defer — so "we moved", "we were sent elsewhere" and "nothing happened" are
  distinguishable at the call site.
- **Results.** A typed `ResultKey<T>` declared next to the producing feature's routes, so the two
  sides cannot disagree about the type. `ResultEffect` runs when the waiting screen is uncovered,
  because Navigation3 composes only the current scene.
- **Arguments.** Lifetime derived from the back stack rather than counted. A count released on
  dispose reaches zero one push early, for the same reason.
- **Back handling.** A screen-level handler only fires while its screen is composed, so it cannot
  stop a jump that skips the screen. A transition guard can, because it sees every stack change.
- **Guards.** `RouteGuard` for a rule about destinations, `NavigationGuard` for a rule about the
  transition, and `GuardVerdict.Deferred` for a rule that cannot answer yet. The PIN example uses a
  `TransientRoute` for its placeholder so a restored stack cannot come back showing a prompt with
  nothing left to answer it.
- **Nested navigation.** Both examples mount a second host inside one entry of the first. The
  per-tab one also shows what using an application-wide guard in a nested host requires: the nested
  host must register the route the guard substitutes, or Navigation3's fallback throws on a key it
  has no entry for.
- **Deep links.** Resolving a link is the root state holder's job — a host can be mounted anywhere,
  but there is one link stream for the whole app. The resolved stack still passes through the
  guards, because the host resolves whatever it is handed before rendering it.
