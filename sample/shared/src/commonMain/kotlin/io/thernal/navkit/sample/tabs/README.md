# Nested navigation — tabs

Catalog group **Nested navigation**: [One host, tabs as its stack](#one-host-tabs-as-its-stack).

A `NavigationHost` can be mounted inside one entry of another. That is how a bottom bar, a wizard, or
any flow with a back stack of its own is built. This example mounts a second host inside a root entry,
makes the tabs that host's stack — **one entry per tab, no depth inside a tab** — and protects one of
them with the application's own guard.

| File | What is in it |
|---|---|
| [`TabsRoutes.kt`](TabsRoutes.kt) | the outer route, the three tab routes, the guarded `SettingsTab` |
| [`SingleHostTabsViewModel.kt`](SingleHostTabsViewModel.kt) / [`SingleHostTabsScreen.kt`](SingleHostTabsScreen.kt) | tabs as one stack |
| [`SettingsTabViewModel.kt`](SettingsTabViewModel.kt) | a tab's own state, scoped so a tab switch does not clear it |
| [`TabsBindings.kt`](TabsBindings.kt) | registers the outer screen in the root host |

## One host, tabs as its stack

The outer screen is an ordinary root entry. Inside it, a bottom bar and a second host whose stack is
owned by an entry-scoped ViewModel:

```kotlin
class SingleHostTabsViewModel : ViewModel() {
    private val state = MutableStateFlow<ImmutableList<Route>>(persistentListOf(HomeTab))
    val backStack: StateFlow<ImmutableList<Route>> = state.asStateFlow()

    // Replacing rather than pushing is what makes the bar a tab bar.
    fun select(tab: SingleHostTab) { state.value = persistentListOf(tab) }

    fun onBackStackChange(next: ImmutableList<Route>) { state.value = next }
}
```

```kotlin
@Composable
fun SingleHostTabsScreen(session: SessionStore) {
    val model: SingleHostTabsViewModel = viewModel { SingleHostTabsViewModel() }
    val backStack by model.backStack.collectAsState()

    Scaffold(bottomBar = { /* NavigationBar calling model.select(tab) */ }) { insets ->
        NavigationHost(
            params = NavigationHostParams(backStack = backStack, onBackStackChange = model::onBackStackChange),
        ) {
            navEntry<HomeTab> { TabBody(title = "Home", body = "…") }
            navEntry<SearchTab> { TabBody(title = "Search", body = "…") }
            navEntry<SettingsTab> { SettingsTabBody(tabsOwner = tabsOwner) }

            // Nothing here pushes SignInRoute — the app-wide guard substitutes it. It must be registered anyway.
            navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session) }
        }
    }
}
```

What to notice:

- **The nested host provides its own `LocalNavigator`.** A screen below calls `push` and lands in the
  tab host's stack, not the application's. To send something to the host *above* — a full-screen route
  over the bar — read `LocalNavigator.current` before mounting and pass it down.
- **There is one stack to think about, and it is one entry deep.** Selecting a tab replaces it; there is
  nothing to push onto a tab, so back always leaves the example through the root host. Depth inside a tab
  would need a stack per tab, and that shape loses every hidden entry's state.
- **The stack's owner is a ViewModel scoped to the root entry**, so it lives while the example is on the
  root stack and is cleared when the example is popped.
- A nested host over its own route type registers its entries inline; `NavigationGraphProvider`s plug
  into hosts typed `Route`.

## Protecting a tab is marking its route

The Settings tab carries the marker the application's `AuthGuard` already narrows to:

```kotlin
data object SettingsTab : SingleHostTab, AuthGuarded
```

- **Application-wide guards apply to nested stacks too.** Selecting Settings while signed out hands the
  host `[SettingsTab]`; the host resolves it through the guards before rendering, and a sign-in screen
  appears inside this host, not the application's.
- **Which is why the nested host registers `SignInRoute`.** A guard can put its substitute into any
  host's stack, and a host with no entry for it throws: `No entry is registered for <route> in this
  NavigationHost …`. A host given a `NavigationHostParams.fallback` renders that instead, which is a
  safety net for links and notifications, not a substitute for registering what a guard can insert.

## Where a tab's ViewModel has to live

A tab switch replaces this host's stack, so from Navigation3's side the tab that was showing was
**popped** — and a pop clears that entry's `rememberSaveable` state and its entry-scoped ViewModels.
A tab whose state is scoped to its own entry therefore comes back empty every time, which is almost
never what a tab bar means.

So the state is scoped one level up, to the entry that mounts the tabs. The host publishes that owner
as `LocalHostViewModelStoreOwner` — it reads it before `NavDisplay`, where `LocalViewModelStoreOwner`
is still the mount point rather than an entry's own:

```kotlin
// inside the tab's entry
val model: SettingsTabViewModel = viewModel(
    viewModelStoreOwner = checkNotNull(LocalHostViewModelStoreOwner.current),
    key = "settings",
) { SettingsTabViewModel() }
```

`key` keeps each tab's state its own when several tabs scope to the same owner.

| Where the ViewModel is scoped | Survives a tab switch | Cleared when |
|---|---|---|
| the tab's own entry (the default `viewModel {}`) | no | the tab is left, because leaving is a pop |
| the entry that mounts the tabs (this example) | yes | the tabs screen is popped off the root host |
| the application graph | yes | never — a leak for anything tab-shaped |

The middle row is the lifetime a tab actually has. Reach for the first only when a screen's state
genuinely should die with the screen — a pushed detail on the host above, for instance.

**Try it:** open *One host, tabs as its stack*. Go to Settings signed out — the sample starts signed
out, and *Members area* toggles it — and the sign-in screen appears inside the tab; sign in and it
continues to Settings. Press *Make an edit* twice, switch to Home and come back: the count is still
two. Leave the example and open it again: it is zero.

## Doing this in your app

1. Give the tab screen an entry-scoped ViewModel that owns the tab stack and a plain
   `onBackStackChange` setter.
2. Mount `NavigationHost` inside the entry and register every tab route inline — plus every route an
   application-wide guard can substitute into this stack.
3. Scope a tab's state to `LocalHostViewModelStoreOwner`, with a `key` per tab. Entry scoping is for
   screens that should die with the screen, which a tab is not.
4. To command the **outer** host from inside a tab — say, to open a full-screen route over the bar — read
   `LocalNavigator.current` before mounting the nested host and pass it down.
5. Keep nesting to one level; each mounted host is a `NavDisplay` with its own state holders and guard
   revalidation.

## Pitfalls

- **An unregistered guard substitute** in a nested host — `No entry is registered for …`.
- **Expecting an entry's `rememberSaveable` or ViewModel to survive a tab switch** — the entry was popped
  from the host's point of view. Scope the tab's state above the host instead.
- **Arguments scoped to routes that live only inside a tab** — only the outermost host prunes, against its
  own stack; scope to the route that mounts the tabs.
- **A back interceptor registered inside a tab** is also consulted by the outer host's `popBack()` — one
  `BackDispatcher` serves every host.
- **Pushing through the inner navigator when the outer one was meant** — `LocalNavigator` is always the
  nearest host's.

## Read more

- [Nested hosts and tabs](../../../../../../../../../../navigation/api/README.md#nested-hosts-and-tabs) in the API guide
- [Guards](../guards/README.md) — the `AuthGuard` this example reuses
- [Known limitations](../../../../../../../../../../navigation/api/README.md#known-limitations) — guard scope across hosts
- [All examples](../../../../../../../../../README.md#the-examples)
