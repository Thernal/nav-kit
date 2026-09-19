# nav-kit

A Compose Multiplatform navigation layer built on **Navigation3** — routes, a stateless navigator
command surface, a render contract for mounting a back stack, guards, deep links, back handling and
cross-screen results, split across `api` / `impl` / `wiring` so nothing depends on a concrete
implementation or on a dependency-injection framework.

Ported from the `core/navigation` module of an Android-only app; what changed on the way across is
recorded in [`navigation/README.md`](navigation/README.md) → "What changed in the port".

## Documentation

| I want to… | Read |
|---|---|
| use the kit — every public contract, task by task, with the rules that matter | [`navigation/api/README.md`](navigation/api/README.md) |
| see each capability running, and set the kit up in my own app | [`sample/README.md`](sample/README.md), then the README in each example package |
| understand why a contract has the shape it has | [`navigation/README.md`](navigation/README.md) |
| know what is still open in the design | [`docs/todos/arguments.md`](docs/todos/arguments.md) |
| give an AI agent the same knowledge | [`skills/`](skills/README.md) — see below |

## For AI agents

**Helping a project that uses nav-kit?** The usage skill lives in [`skills/nav-kit/`](skills/nav-kit/SKILL.md).
Start at `skills/nav-kit/SKILL.md`: it states the model, the checks to run in the project first, and routes
each task to one file under `skills/nav-kit/references/` — `setup.md`, `screens-and-navigation.md`,
`guards.md`, `passing-data.md`, `back-handling.md`, `deep-links.md`, `events-and-testing.md`,
`troubleshooting.md`. Read it in place, or install it so your agent loads it on its own:

```sh
git clone --depth 1 https://github.com/Thernal/nav-kit.git /tmp/nav-kit
mkdir -p .claude/skills && cp -R /tmp/nav-kit/skills/nav-kit .claude/skills/   # Claude Code; other runtimes: their skills directory
```

[`skills/README.md`](skills/README.md) lists every skill and the install options. The skill restates
[`navigation/api/README.md`](navigation/api/README.md) for an agent; when in doubt, that README and the code
under `navigation/api` are the source of truth.

**Working on nav-kit itself?** The skill above is not for you. Read this README, `navigation/README.md` for
the design, and [`.agents/workspace.md`](.agents/workspace.md) for this repository's delivery conventions and
where each kind of artifact belongs. `.claude/skills/` holds vendored workflow skills (task planning, git
delivery) — they are not about navigation.

## Layout

| Path | What |
|---|---|
| `navigation/api` | Routes, the navigator command surface, the `NavigationHost` render contract, guard/deep-link/result/back contracts, the navigation event stream. Depends on no implementation. |
| `navigation/impl` | The Navigation3 host, the back-stack navigator, overlay scenes, animations, deep-link parsing and dispatch, guards, results. |
| `navigation/wiring` | The worked example of installing the above into an application graph, with [Metro](https://github.com/ZacSweers/metro). `api` and `impl` name no container, so an app on a different one replaces just this module. |
| `sample/` | A runnable Android and iOS app with a simple and an advanced example of every capability — see [`sample/README.md`](sample/README.md). |
| `skills/` | Agent skills for projects that use the kit — see [For AI agents](#for-ai-agents). |
| `docs/` | Design notes and open work. |
| `build-logic/convention` | Five convention plugins — `kmp.library`, `compose`, `injection`, `android.application` for the sample app, and a `quality` one with no plugin id that the first and the fourth apply. A module names capabilities, never versions. |
| `build-logic/detekt-rules` | The project's own Detekt rules — see "Static analysis" below. |

Modules are discovered from the tree: any directory under `navigation/` or `sample/` with a
`build.gradle.kts` is a Gradle project, so `settings.gradle.kts` has no list to keep in sync.

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
fun AppRoot(graph: AppGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        val root: RootViewModel = viewModel { RootViewModel() }
        val backStack by root.backStack.collectAsState()

        NavigationHost(
            params = NavigationHostParams(
                backStack = backStack,
                onBackStackChange = root::onBackStackChange,
            ),
        ) {
            navEntry<AuthRoute.SignIn> { SignInScreen() }
            bottomSheetEntry<ProfileRoute.Edit> { ProfileEditSheet() }
        }
    }
}
```

`NavigationHost` resolves its renderer from `LocalNavigationHostRenderer`, which the app installs
once at its composition root — with the Metro wiring in place, as part of the graph's collected
`Set<ProvidedValue<*>>`. Without the renderer installed the host draws nothing rather than crashing,
which is what makes an isolated preview of a screen compose.

The full setup — graph, root back-stack owner, deep links, platform entry points — is in
[`navigation/api/README.md`](navigation/api/README.md#installing), and running in
[`sample/`](sample/README.md#set-nav-kit-up-in-your-own-app).

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
