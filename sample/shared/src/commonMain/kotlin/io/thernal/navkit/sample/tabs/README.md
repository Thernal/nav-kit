# Nested navigation — tabs

Catalog group **Nested navigation**: [One host, tabs as its stack](#simple-one-host-tabs-as-its-stack)
(simple) and [A stack per tab](#real-life-a-stack-per-tab) (real life).

A `NavigationHost` can be mounted inside one entry of another. That is how a bottom bar, a wizard, or
any flow with a back stack of its own is built. Both examples here mount a second host inside a root
entry and differ in one decision: whether each tab keeps its own depth.

| | One host, tabs as its stack | A stack per tab |
|---|---|---|
| State owner | `SingleHostTabsViewModel`: one `ImmutableList<Route>` | `PerTabStacksViewModel`: `Map<Tab, ImmutableList<Route>>` + the selected tab |
| Selecting a tab | replaces the stack with that tab | hands the host that tab's list |
| Depth after switching away and back | gone | kept |
| Re-selecting the active tab | — | resets it to its root |
| Guards | — | the Admin tab is protected by the app-wide `AuthGuard` |

| File | What is in it |
|---|---|
| [`TabsRoutes.kt`](TabsRoutes.kt) | the two outer routes, the tab routes of each example, the guarded `AdminRoute`, the `Tab` enum |
| [`SingleHostTabsViewModel.kt`](SingleHostTabsViewModel.kt) / [`SingleHostTabsScreen.kt`](SingleHostTabsScreen.kt) | tabs as one stack |
| [`PerTabStacksViewModel.kt`](PerTabStacksViewModel.kt) / [`PerTabStacksScreen.kt`](PerTabStacksScreen.kt) | a stack per tab, one of them guarded |
| [`TabsBindings.kt`](TabsBindings.kt) | registers the two outer screens in the root host |

## Simple: One host, tabs as its stack

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
fun SingleHostTabsScreen() {
    val model: SingleHostTabsViewModel = viewModel { SingleHostTabsViewModel() }
    val backStack by model.backStack.collectAsState()

    Scaffold(bottomBar = { /* NavigationBar calling model.select(tab) */ }) { insets ->
        NavigationHost(
            params = NavigationHostParams(backStack = backStack, onBackStackChange = model::onBackStackChange),
        ) {
            navEntry<HomeTab> { TabBody(title = "Home", body = "…") }
            navEntry<SearchTab> { TabBody(title = "Search", body = "…") }
            navEntry<SettingsTab> { SettingsTabBody() }
            navEntry<SettingsDetail> { route -> TabBody(title = route.section, body = "Pushed inside the tab.") }
        }
    }
}
```

What to notice:

- **The nested host provides its own `LocalNavigator`.** `SettingsTabBody` calls `navigator.push(…)` and
  lands in the tab host's stack, not the application's. The same screen mounted in the root host would
  drive the root host.
- **There is one stack to think about:** selecting a tab replaces it, going deeper pushes, back pops.
  When the tab host has nothing left to pop, back goes to the root host and leaves the example.
- **The stack's owner is a ViewModel scoped to the root entry**, so it lives while the example is on the
  root stack and is cleared when the example is popped.
- A nested host over its own route type registers its entries inline; `NavigationGraphProvider`s plug
  into hosts typed `Route`.

**Try it:** open *One host, tabs as its stack*, go to Settings, *Open notifications*, switch to Home and
back to Settings: the depth is gone, because selecting a tab replaced the stack.

## Real life: A stack per tab

The stacks live in the screen's ViewModel, and only the selected one is handed to the host. Nothing in
the kit had to change to allow it: the host is controlled, so switching tabs is handing it another list.

```kotlin
class PerTabStacksViewModel : ViewModel() {
    private val stacks = MutableStateFlow<Map<Tab, ImmutableList<Route>>>(
        mapOf(
            Tab.FEED to persistentListOf(FeedList),
            Tab.SAVED to persistentListOf(SavedList),
            Tab.ADMIN to persistentListOf(AdminDashboard),
        ),
    )
    private val selected = MutableStateFlow(Tab.FEED)

    fun select(tab: Tab) {
        if (selected.value == tab) {
            stacks.value = stacks.value + (tab to persistentListOf(rootOf(tab)))  // re-select resets
            return
        }
        selected.value = tab
    }

    // Writes go to whichever tab is showing.
    fun onBackStackChange(next: ImmutableList<Route>) {
        stacks.value = stacks.value + (selected.value to next)
    }
}
```

```kotlin
NavigationHost(
    params = NavigationHostParams(backStack = stacks.getValue(tab), onBackStackChange = model::onBackStackChange),
) {
    navEntry<FeedList> { ListBody(title = "Feed", open = { id -> FeedItem(id) }) }
    navEntry<FeedItem> { route -> DetailBody(title = "Feed item ${route.id}", note = ITEM_NOTE) }
    navEntry<SavedList> { ListBody(title = "Saved", open = { id -> SavedItem(id) }) }
    navEntry<SavedItem> { route -> DetailBody(title = "Saved item ${route.id}", note = ITEM_NOTE) }
    navEntry<AdminDashboard> { DetailBody(title = "Admin dashboard", note = ADMIN_NOTE) }

    // Nothing here pushes SignInRoute — the app-wide guard substitutes it. It must be registered anyway.
    navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session) }
}
```

**Protecting a whole tab is marking its routes.** The Admin tab's routes carry the marker the
application's `AuthGuard` already narrows to:

```kotlin
sealed interface AdminRoute : Route, AuthGuarded
data object AdminDashboard : AdminRoute
```

What to notice:

- **Application-wide guards apply to nested stacks too.** Selecting Admin while signed out hands the host
  `[AdminDashboard]`; the host resolves it through the guards before rendering, and a sign-in screen
  appears inside this host, not the application's.
- **Which is why the nested host registers `SignInRoute`.** A guard can put its substitute into any
  host's stack. A host with no entry for it hands Navigation3 a key its fallback throws on:
  `IllegalStateException: Unknown screen …`.
- **The routes of a hidden tab survive the switch; its entries' state does not.** When the host is
  handed another list, Navigation3 treats the entries that left it as popped and clears their
  `rememberSaveable` state and entry-scoped ViewModels. Keep what a tab must remember across a switch in
  the ViewModel that owns the stacks, or in a repository.
- **A tab switch is a stack the host did not write.** A guard deferral waiting in this host is abandoned
  by it, as it would be by a deep link.

**Try it:** open *A stack per tab*, open Feed item 2, switch to Saved, open item 3, switch back to Feed:
item 2 is still on top. Tap Feed again: it resets to the list. Select Admin while signed out — the
sample starts signed out, and *Members area* toggles it: the sign-in screen appears inside the tab, and
signing in continues to the dashboard.

## Doing this in your app

1. Give the tab screen an entry-scoped ViewModel that owns the tab stack(s) and a plain
   `onBackStackChange` setter.
2. Mount `NavigationHost` inside the entry and register every tab route inline — plus every route an
   application-wide guard can substitute into this stack.
3. Pick the shape: replace the stack on selection (simple) or keep a map of stacks (depth survives).
4. To command the **outer** host from inside a tab — say, to open a full-screen route over the bar — read
   `LocalNavigator.current` before mounting the nested host and pass it down.
5. Keep nesting to one level; each mounted host is a `NavDisplay` with its own state holders and guard
   revalidation.

## Pitfalls

- **An unregistered guard substitute** in a nested host — `Unknown screen`.
- **Expecting `rememberSaveable` in a hidden tab to survive a switch** — the entry was popped from the
  host's point of view.
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
