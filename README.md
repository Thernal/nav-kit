# nav-kit

A Compose Multiplatform navigation layer built on **Navigation3** — routes, a stateless navigator
command surface, a render contract for mounting a back stack, guards, deep links, back handling and
cross-screen results, split across `api` / `impl` / `wiring` so nothing depends on a concrete
implementation or on a dependency-injection framework.

Ported from the `core/navigation` module of an Android-only app; what changed on the way across is
recorded in [`navigation/README.md`](navigation/README.md) → "What changed in the port".

## Layout

| Path | What |
|---|---|
| `navigation/api` | Routes, the navigator command surface, the `NavigationHost` render contract, guard/deep-link/result/back contracts, the navigation event stream. Depends on no implementation. |
| `navigation/impl` | The Navigation3 host, the back-stack navigator, overlay scenes, animations, deep-link parsing and dispatch, guards, results. |
| `navigation/wiring` | The worked example of installing the above into an application graph, with [Metro](https://github.com/ZacSweers/metro). `api` and `impl` name no container, so an app on a different one replaces just this module. |
| `build-logic/convention` | Four convention plugins — `kmp.library`, `compose`, `injection`, and a `quality` one with no plugin id that the first applies. A module names capabilities, never versions. |
| `build-logic/detekt-rules` | The project's own Detekt rules — see "Static analysis" below. |

Modules are discovered from the tree: any directory under `navigation/` with a `build.gradle.kts`
is a Gradle project, so `settings.gradle.kts` has no list to keep in sync.

## Targets

`android`, `iosArm64`, `iosSimulatorArm64` — the target set every Navigation3 artifact in the
catalog publishes. Those artifacts publish `desktop`, `js` and `wasmJs` variants as well, so adding one of those
targets is a line in `KmpLibraryConventionPlugin` — not verified here, since nothing in this
repository builds for them yet. `iosX64` has no `navigation3-ui` variant, so it is deliberately
absent rather than merely unlisted.

`commonTest` runs on the Android host-test (JVM) compilation and on the iOS simulator, so every
test in this repository is executed twice on different backends.

## Building

```
./gradlew build          # compile every target, run the tests on both, and run Detekt
./gradlew :navigation:impl:allTests
./gradlew detektMainAndroid --auto-correct   # apply the ktlint-formatting fixes
```

**The Gradle daemon runs on JDK 21**, declared in `gradle/gradle-daemon-jvm.properties`: Metro's
Gradle plugin is compiled for JVM 21, so an older daemon cannot even configure the build. The file
carries download URLs, so Gradle provisions that JDK into its own cache on a machine that has none
— nothing has to be installed by hand. Modules themselves target the `jvm` version in
`gradle/libs.versions.toml` through `jvmToolchain`, which is a separate setting.

`local.properties` must point at an Android SDK (`sdk.dir=…`); it is git-ignored.

## Using it

```kotlin
@Composable
fun AppRoot(viewModel: RootViewModel) {
    val backStack by viewModel.backStack.collectAsStateWithLifecycle()

    NavigationHost(
        params = NavigationHostParams(
            backStack = backStack,
            onBackStackChange = viewModel::updateBackStack,
        ),
    ) {
        navEntry<AuthRoute.SignIn> { SignInScreen() }
        bottomSheetEntry<ProfileRoute.Edit> { ProfileEditSheet() }
    }
}
```

`NavigationHost` resolves its renderer from `LocalNavigationHostRenderer`, which the app installs
once at its composition root. With the Metro wiring in place that is a single line over the graph's
collected `Set<ProvidedValue<*>>`:

```kotlin
CompositionLocalProvider(*graph.providedValues.toTypedArray()) { AppRoot(viewModel) }
```

Without the renderer installed the host draws nothing rather than crashing, which is what makes an
isolated preview of a screen compose.

See [`navigation/README.md`](navigation/README.md) for routes, guards, deep links, results and the
module boundary rules.

## Static analysis

Detekt runs as part of `check`, configured once in `config/detekt/detekt.yml`, and **findings fail
the build** — the repository starts with none, and a warning nobody has to clear is a rule that
decays. A genuinely wrong finding is silenced in the config or with `@Suppress`, where the decision
is visible in review.

Alongside the standard rules and the ktlint wrapper, `build-logic/detekt-rules` ships six rules of
its own, in the `project` rule set:

| Rule | Enforces |
|---|---|
| `LayerPackageRequired` | every file of an `api`/`impl` module lives in its `data`, `domain` or `presentation` package |
| `LayerPackageBoundary` | inside one module, `domain` imports neither of the other two, and `data`/`presentation` never import each other |
| `UnsafeCollectionIndexAccess` | `list[i]` / `list.get(i)` give way to `getOrNull(i)` |
| `ExpressionBodyNotAllowed` | function bodies are `{ … }`, not `= …` |
| `MultilineConstructorRequired` | a primary constructor with 2+ parameters puts each on its own line |
| `PreviewMustBePrivate` | a `@Preview` function never widens a module's public API |

The two layer rules are what make the `api`/`impl`/`wiring` split checkable rather than a
convention: they read the package name, so they apply to any module whose root package ends in
`api` or `impl`, and skip `wiring` — a binding container is none of the three layers.

Their tests live in the included build, so they are not picked up by a module's `test` task;
`./gradlew detektRulesTest` runs them, and the root `test` task depends on it.
