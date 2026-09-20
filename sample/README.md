# sample

A runnable demonstration of everything in `navigation/`, on Android and iOS, from one shared
composition. Each capability has a **simple** example — the smallest thing that works — and
an **advanced** one — the shape the problem actually takes in an application. Every example package has
a README of its own that walks through the code, says what to notice, and how to try it.

It is also the reference integration: [Set nav-kit up in your own app](#set-nav-kit-up-in-your-own-app)
below follows it step by step.

- The complete API guide: [`navigation/api/README.md`](../navigation/api/README.md)
- Why the kit is shaped the way it is: [`navigation/README.md`](../navigation/README.md)

## Running it

```sh
./gradlew :sample:app:installDebug          # Android
open sample/iosApp/iosApp.xcodeproj         # iOS, then run the iosApp scheme
```

The Xcode project builds the shared framework itself through a run-script phase
(`./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode`), so there is no separate Gradle step.
It sets `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` because the kit targets `iosArm64` and
`iosSimulatorArm64` only — the two every Navigation3 artifact in the catalog publishes.

Both apps accept `navkit://` links from the platform, cold or warm, and hand them to the same ingress
the in-app playground uses:

```sh
adb shell am start -a android.intent.action.VIEW -d navkit://orders/77    # Android
xcrun simctl openurl booted navkit://orders/77                           # iOS
```

## What is where

| Module | What it is |
|---|---|
| `shared` | Every screen, route, guard, handler and binding. Android and iOS run this unchanged. |
| `app` | An Android application: an `Application` that owns the graph, one activity that hosts the composition and forwards link intents. |
| `iosApp` | A SwiftUI shell whose only view is the shared composition, and whose `onOpenURL` forwards links. |

Inside `shared/src/commonMain/kotlin/io/thernal/navkit/sample/`:

| Package | Holds |
|---|---|
| [`app/`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app) | the composition root, the graph, the root back-stack owner, the deep-link log — the integration |
| [`catalog/`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/catalog) | the index screen, built from the examples the graph collected |
| [`ui/`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/ui) | the theme, each capability's colour, the screen frame every example uses — a realistic screen on top, a "How it works" panel with the live navigation state below it — and the surface every bottom sheet is drawn in |
| `basics/`, `results/`, `arguments/`, `backoverride/`, `guards/`, `tabs/`, `deeplinks/`, `sheets/` | one capability each — see below |

## The examples

| Group | Simple | Advanced | Teaches |
|---|---|---|---|
| [Navigation](shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/README.md) | Push and pop | Order flow | the command surface: `push`, `popBack`, `navigate` with a predicate, `popBackTo`, `replaceAll`, and reading `NavigationOutcome`; how a feature registers its screens |
| [Results](shared/src/commonMain/kotlin/io/thernal/navkit/sample/results/README.md) | Pick a colour | Review request | a typed value handed back to a screen already on the stack: `resultKey`, `post`, `ResultEffect`, `pending`, `clear` |
| [Arguments](shared/src/commonMain/kotlin/io/thernal/navkit/sample/arguments/README.md) | One value, two screens | Checkout draft | a value shared forwards across a flow, its lifetime derived from the stack: `argumentKey`, `put`, `whileInStack` |
| [Back handling](shared/src/commonMain/kotlin/io/thernal/navkit/sample/backoverride/README.md) | Confirm before leaving | Unsaved work | `NavigationBackHandler` for "confirm on back", a transition guard for "refuse every way out" |
| [Guards](shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/README.md) | Members area | 401 and a PIN | `RouteGuard` with a redirect that keeps intent, `invalidations`, `GuardVerdict.Deferred`, `TransientRoute` |
| [Nested navigation](shared/src/commonMain/kotlin/io/thernal/navkit/sample/tabs/README.md) | One host, tabs as its stack | — | a host inside a host, one entry per tab; an application-wide guard inside a tab; where a tab’s ViewModel has to be scoped; a host's own `transitionSpec` |
| [Deep links](shared/src/commonMain/kotlin/io/thernal/navkit/sample/deeplinks/README.md) | One link, one route | Campaign links | registered link bases, handlers, stacks rather than destinations, the source, a link meeting a guard |
| [Bottom sheets](shared/src/commonMain/kotlin/io/thernal/navkit/sample/sheets/README.md) | Share sheet | Payment method | `bottomSheetEntry`, a sheet as one more entry on the same stack, a run of sheets as one panel whose height animates between steps, the surface the app supplies once |

## Set nav-kit up in your own app

Five steps, each pointing at the file in this sample that does it. The generic version, including
wiring without a DI framework, is in [Installing](../navigation/api/README.md#installing).

### 1. Depend on the three modules

[`shared/build.gradle.kts`](shared/build.gradle.kts): the contracts (`api`), the implementation that
backs them (`implementation`), and the Metro bindings that install both (`implementation`). The module
that declares the graph also applies the Metro plugin. A feature module needs only
`:navigation:api`.

```kotlin
commonMain.dependencies {
    api(projects.navigation.api)
    implementation(projects.navigation.impl)
    implementation(projects.navigation.wiring)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.lifecycle.viewmodel.compose)
}
```

### 2. Build one graph for the whole process

[`app/SampleGraph.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleGraph.kt)
asks for what the root needs, by contract, and names no feature:

```kotlin
@DependencyGraph(AppScope::class)
interface SampleGraph {
    val providedValues: Set<ProvidedValue<*>>          // renderer, results, arguments — from NavigationWiring
    val graphProviders: Set<NavigationGraphProvider>   // every feature's screens
    val examples: Set<SampleExample>                   // the sample's own index
    val deepLinkEvents: DeepLinkEvents
    val deepLinkDispatcher: DeepLinkDispatcher
    val deepLinkIngress: DeepLinkIngress
    val deepLinkLog: DeepLinkLog
}
```

[`app/SampleBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleBindings.kt)
declares the application's own multibindings (`@Multibinds(allowEmpty = true)`) and the two
`DeepLinkBase`s its links start with. The kit's multibindings — guards, deep-link handlers, deep-link
bases, event sinks — are declared by `NavigationWiring`.

The graph is created **once per process** and handed to the composition:
[`SampleApplication.kt`](app/src/main/kotlin/io/thernal/navkit/sample/android/SampleApplication.kt)
on Android, a process-wide `lazy` in
[`MainViewController.kt`](shared/src/iosMain/kotlin/io/thernal/navkit/sample/app/MainViewController.kt)
on iOS. The sample used to remember the graph in the composition: a rotation rebuilt the session, the
drafts, the argument store and the results mailbox, while the back stack — in a ViewModel — survived and
pointed at state that no longer existed.

### 3. Write the composition root

[`app/SampleApp.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleApp.kt) does the
three things every application does:

```kotlin
@Composable
fun SampleApp(graph: SampleGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {     // 1. locals, in one set
        MaterialTheme {
            val root: RootViewModel = viewModel { RootViewModel() }               // 2. the stack's owner
            val backStack by root.backStack.collectAsState()

            LaunchedEffect(graph) {                                               // 3. links, resolved once
                graph.deepLinkEvents.links.collect { incoming ->
                    val outcome = graph.deepLinkDispatcher.dispatch(raw = incoming.uri, source = incoming.source)
                    graph.deepLinkLog.record(link = incoming, outcome = outcome)
                    if (outcome is DeepLinkOutcome.Navigate) {
                        root.onDeepLink(outcome.routes)
                    }
                }
            }

            NavigationHost(
                params = NavigationHostParams(
                    backStack = backStack,
                    onBackStackChange = root::onBackStackChange,
                    fallback = ::unknownRouteEntry,                                   // 4. a route nothing registers
                ),
            ) {
                for (provider in graph.graphProviders) {
                    with(provider) { provide() }
                }
            }
        }
    }
}
```

- **Composition locals arrive in one set.** A screen reaches the navigator, the results mailbox or the
  argument store without importing the navigation module, and adding a capability to the kit does not
  change this file.
- **Screens arrive from the graph.** The root registers entries it has never heard of; the catalog
  works the same way over `Set<SampleExample>`.
- **Links are resolved here, once.** A host can be mounted anywhere; there is one link stream.
- **An unregistered route is a screen, not a crash.** `fallback` renders `UnknownRouteScreen`; without
  it the host throws and names the route.

### 4. Own the root back stack outside the host

[`app/RootViewModel.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/RootViewModel.kt):
a `StateFlow<ImmutableList<Route>>`, a **plain** `onBackStackChange` setter — every command, guard
correction and settled deferral arrives through it — and `onDeepLink(routes)`, which replaces the stack.
It is typed `Route` rather than a sealed application type, because the entries come from feature
providers and a closed type would import every feature.

### 5. Contribute each feature

Every example package ends in a `*Bindings.kt` of the same shape — see
[`basics/BasicsBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/basics/BasicsBindings.kt):

| Contribute `@IntoSet` | Type | Example |
|---|---|---|
| the feature's screens | `NavigationGraphProvider` | every package |
| an access or transition rule | `NavigationGuard` | [`GuardsBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/guards/GuardsBindings.kt), [`BackBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/backoverride/BackBindings.kt) |
| the pages it opens from links | `DeepLinkHandler` | [`DeepLinksBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/deeplinks/DeepLinksBindings.kt) |
| the schemes and domains the app's links start with — once, by the application | `DeepLinkBase` | [`app/SampleBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleBindings.kt) |
| an observer of navigation | `NavigationEventSink` | [`app/SampleBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleBindings.kt) — prints every command |

State a guard reads — a session, a draft — is provided `@SingleIn(AppScope::class)`, because a guard
runs outside composition and cannot see what a screen remembers.

### Platform entry points

The platform's only navigation duty is to publish links; everything else is shared. Every scheme the
platform declares is also registered as a `DeepLinkBase` in
[`app/SampleBindings.kt`](shared/src/commonMain/kotlin/io/thernal/navkit/sample/app/SampleBindings.kt) —
a link whose scheme or domain is not registered resolves to `NotFound`.

**Android** — [`MainActivity.kt`](app/src/main/kotlin/io/thernal/navkit/sample/android/MainActivity.kt)
and [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml):

- the activity is `android:launchMode="singleTop"`, so a link arriving while the app is open reaches
  `onNewIntent` instead of a second activity with a second composition;
- a `VIEW` intent filter with `DEFAULT`, `BROWSABLE` and `<data android:scheme="navkit" />`;
- `onCreate` publishes `intent` only when `savedInstanceState == null` — a recreated activity still
  carries its launch intent, and publishing it again would apply the link twice;
- `onNewIntent` calls `setIntent(intent)` and publishes it;
- the `<application>` sets `android:enableOnBackInvokedCallback="true"`, which is what lets the
  predictive-back gesture reach the host — without it back still works, but the outgoing screen jumps
  instead of following the finger, and `predictivePopTransitionSpec` never runs.

**iOS** — [`iOSApp.swift`](iosApp/iosApp/iOSApp.swift), [`ContentView.swift`](iosApp/iosApp/ContentView.swift),
[`Info.plist`](iosApp/iosApp/Info.plist) and
[`MainViewController.kt`](shared/src/iosMain/kotlin/io/thernal/navkit/sample/app/MainViewController.kt):

- `ContentView` wraps `MainViewController()` — a `ComposeUIViewController` over the same `SampleApp`;
- `.onOpenURL` calls the Kotlin `handleDeepLink(url:)`, which publishes to the ingress;
- `Info.plist` declares the `navkit` scheme under `CFBundleURLTypes`, and sets
  `CADisableMinimumFrameDurationOnPhone` to `true` — Compose Multiplatform checks that key on its first
  frame and the app closed at launch without it.

### Checklist

- [ ] One graph per process, handed to the composition root.
- [ ] The root installs `graph.providedValues`, owns its stack in a ViewModel with a plain setter,
      registers every `NavigationGraphProvider`, and resolves deep links.
- [ ] Every route a host can show — including sign-in screens and placeholders that guards put there —
      has exactly one entry in that host.
- [ ] Guards, deep-link handlers and event sinks are contributed `@IntoSet`.
- [ ] Every scheme and domain in the manifest and `Info.plist` is registered as a `DeepLinkBase`.
- [ ] Android: `singleTop`, intent filter, publish in `onCreate` (first creation only) and `onNewIntent`,
      `enableOnBackInvokedCallback` for predictive back.
- [ ] iOS: URL type, `onOpenURL` → ingress, `CADisableMinimumFrameDurationOnPhone`.

## What the sample does not show

These are part of the API but have no example here; the API guide covers each.

| Capability | Where to read |
|---|---|
| custom `SceneStrategy`s — a surface the kit does not ship | [Scenes](../navigation/api/README.md#scenes) |
| disabling a transition, and tuning the defaults — `NavigationDefaults` (the tabs example does supply a custom `transitionSpec`) | [Transitions](../navigation/api/README.md#transitions) |
| guards for one host only — `NavigationHostParams.guards` | [Registering guards](../navigation/api/README.md#registering-guards) |
| enum-typed pages — `TypedDeepLinkHandler`, `DeepLinkPage`, `pageOf`, outbound `buildUri` | [Handlers](../navigation/api/README.md#handlers), [Outbound links](../navigation/api/README.md#outbound-links) |
| navigation decided in a ViewModel and replayed in the composable | [Calling from a ViewModel](../navigation/api/README.md#calling-from-a-viewmodel) |
| `buildStack`, `whileRouteInStack`, `popBack(count)` | [Commands](../navigation/api/README.md#commands), [Arguments](../navigation/api/README.md#arguments) |
| unit tests for guards, commands, results, arguments and links | [Testing](../navigation/api/README.md#testing) |
