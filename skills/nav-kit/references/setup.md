# Installing nav-kit into an application

## Contents

1. Dependencies
2. The application graph — with Metro, or by hand
3. The root back-stack owner
4. The composition root
5. Platform entry points
6. Verify

## 1. Dependencies

The modules are not published to a Maven repository; the project builds against them from source
(`navigation/api`, `navigation/impl`, `navigation/wiring` in https://github.com/Thernal/nav-kit, which
also carries the Gradle conventions and version catalog they expect). Targets: `android`, `iosArm64`,
`iosSimulatorArm64` — there is no `iosX64` variant of `navigation3-ui`.

| Module | Add to | As |
|---|---|---|
| `:navigation:api` | every feature module; the app | `api` in the app's shared module, `implementation` elsewhere |
| `:navigation:impl` | the module that builds the graph; modules using `NavigationBackHandler` or `NavAnimations` | `implementation` |
| `:navigation:wiring` | the module declaring the Metro graph — skip when wiring by hand | `implementation` |

The root back stack lives in a ViewModel, so the app module also needs
`androidx.lifecycle:lifecycle-viewmodel-compose` (for `viewModel { }`) and
`kotlinx-collections-immutable`.

## 2. The application graph

### With Metro

`NavigationWiring` is `@BindingContainer @ContributesTo(AppScope::class)`: a graph over `AppScope`
includes it automatically. It declares `Set<NavigationGuard>`, `Set<DeepLinkHandler>`,
`Set<DeepLinkBase>` and `Set<NavigationEventSink>` as empty-allowed multibindings, binds every service
as a singleton, and contributes three `ProvidedValue<*>` — the renderer, results and arguments locals.

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val providedValues: Set<ProvidedValue<*>>
    val graphProviders: Set<NavigationGraphProvider>
    val deepLinkEvents: DeepLinkEvents
    val deepLinkDispatcher: DeepLinkDispatcher
    val deepLinkIngress: DeepLinkIngress
}

// The app declares its own multibinding for screens; the kit does not.
@BindingContainer
@ContributesTo(AppScope::class)
interface AppBindings {
    @Multibinds(allowEmpty = true)
    val graphProviders: Set<NavigationGraphProvider>

    companion object {
        // One per scheme and domain the platform declarations below name. Handlers with no base fail
        // when the dispatcher is built; a link matching no base is NotFound.
        @Provides
        @IntoSet
        fun provideAppSchemeBase(): DeepLinkBase {
            return DeepLinkBase("myapp://")
        }

        @Provides
        @IntoSet
        fun provideWebOriginBase(): DeepLinkBase {
            return DeepLinkBase("https://example.com")
        }
    }
}

fun createAppGraph(): AppGraph {
    return createGraph<AppGraph>()
}
```

Do not bind `Navigator`: a host builds its own per host.

### By hand (any other container, or none)

```kotlin
class Navigation(
    guards: List<NavigationGuard>,
    deepLinkHandlers: Set<DeepLinkHandler>,
    deepLinkBases: Set<DeepLinkBase>,             // e.g. setOf(DeepLinkBase("myapp://"))
    sinks: List<NavigationEventSink> = emptyList(),
) {
    private val bridge = RuntimeDeepLinkBridge()          // ONE object behind ingress and events
    private val arguments = NavigationArgumentsImpl()     // ONE object behind arguments and pruner
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

Imports: `io.thernal.navkit.navigation.impl.data.RuntimeDeepLinkBridge`,
`…impl.domain.argument.NavigationArgumentsImpl`, `…impl.domain.result.NavigationResultsImpl`,
`…impl.domain.deeplink.DeepLinkDispatcherImpl`, `…impl.domain.guard.NavigationGuardRunnerImpl`,
`…impl.domain.back.BackDispatcherImpl`, `…impl.presentation.host.NavigationHostRendererImpl`.

### Lifetime

Create the graph **once per process** — Android: a property of the `Application`; iOS: a top-level
`private val graph by lazy { … }`. Never inside a composable: an Android activity recreation rebuilds it,
resetting sessions, results and arguments while the root back stack (in a ViewModel) survives and shows
screens whose state is gone.

## 3. The root back-stack owner

```kotlin
class RootViewModel : ViewModel() {
    private val mutableBackStack = MutableStateFlow<ImmutableList<Route>>(persistentListOf(HomeRoute))
    val backStack: StateFlow<ImmutableList<Route>> = mutableBackStack.asStateFlow()

    fun onBackStackChange(next: ImmutableList<Route>) {
        mutableBackStack.value = next           // plain setter — no filtering, no re-applying
    }

    fun onDeepLink(routes: List<Route>) {
        if (routes.isNotEmpty()) {
            mutableBackStack.value = routes.toImmutableList()
        }
    }
}
```

Type the root stack `Route`, not a sealed app type: screens come from feature providers.
The initial stack must not be empty.

## 4. The composition root

```kotlin
@Composable
fun App(graph: AppGraph) {
    CompositionLocalProvider(values = graph.providedValues.toTypedArray()) {
        val root: RootViewModel = viewModel { RootViewModel() }
        val backStack by root.backStack.collectAsState()

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

With hand wiring, `values = navigation.providedValues.toTypedArray()` and register entries inline or from
your own provider list. Apply the app's theme inside the provider.

## 5. Platform entry points

The platform only publishes links. Resolving happens at the root above.

### Android

```kotlin
class MyApplication : Application() {
    val graph: AppGraph by lazy { createAppGraph() }
}

class MainActivity : ComponentActivity() {
    private val graph: AppGraph
        get() {
            return (application as MyApplication).graph
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            graph.deepLinkIngress.publish(intent)   // extension in api's androidMain; first creation only
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

`publish(intent)` is `io.thernal.navkit.navigation.api.presentation.deeplink.publish`.

```xml
<!-- <application android:name=".MyApplication"> -->
<activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="myapp" />
    </intent-filter>
</activity>
```

### iOS

```kotlin
// iosMain
private val graph: AppGraph by lazy { createAppGraph() }

fun MainViewController(): UIViewController {
    return ComposeUIViewController { App(graph) }
}

fun handleDeepLink(url: String): Boolean {
    return graph.deepLinkIngress.publish(uri = url, source = DeepLinkSource.EXTERNAL_LINK)
}
```

```swift
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { MainViewControllerKt.MainViewController() }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea(edges: .all)
                .onOpenURL { url in _ = MainViewControllerKt.handleDeepLink(url: url.absoluteString) }
        }
    }
}
```

The scheme in the intent filter must also be a registered `DeepLinkBase` (section 2).

`Info.plist`: `CFBundleURLTypes` with the scheme (registered as a `DeepLinkBase` too), and
`CADisableMinimumFrameDurationOnPhone` = `true` —
Compose Multiplatform checks it on the first frame and the app closes at launch without it. The Kotlin
file name decides the Swift name (`MainViewController.kt` → `MainViewControllerKt`).

## 6. Verify

- The app builds for Android and at least one iOS target.
- The first screen renders. If a host draws nothing, the renderer local is not installed.
- A command answering `Rewritten` over an empty stack means the screen is outside every host.
- A link reaches the app: `adb shell am start -a android.intent.action.VIEW -d myapp://page/1`,
  `xcrun simctl openurl booted myapp://page/1`; cold start and warm (app already open) both work, and
  rotating afterwards does not re-apply it.
- Rotate on Android and confirm guarded state (a session, results, arguments) is still there.
