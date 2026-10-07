package io.thernal.navkit.sample.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.navkit.navigation.api.presentation.host.LocalHostViewModelStoreOwner
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.SlideDirection
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.impl.presentation.host.NavAnimations
import io.thernal.navkit.sample.guards.SessionStore
import io.thernal.navkit.sample.guards.SignInRoute
import io.thernal.navkit.sample.guards.SignInScreen
import io.thernal.navkit.sample.ui.BackArrowButton
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.HowItWorks
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.Topic
import io.thernal.navkit.sample.ui.TopicChip

private val topic = Topic.NESTED_NAVIGATION

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleHostTabsScreen(session: SessionStore) {
    // Read before mounting the nested host: inside it, `LocalNavigator` is the tabs' own.
    val outer = LocalNavigator.current
    val model: SingleHostTabsViewModel = viewModel { SingleHostTabsViewModel() }
    val backStack by model.backStack.collectAsState()
    // A refused tab shows the guard's sign-in screen; the bar still highlights the tab it stands for.
    val selected = backStack.first().let { route -> (route as? SignInRoute)?.next ?: route }
    val title = tabsOf().firstOrNull { tab -> tab.route == selected }?.label ?: "Tabs"

    // Slides towards the picked tab: one to the right slides left, one to the left slides right.
    val tabTransition = remember {
        NavAnimations.slideTo<Route>(
            direction = {
                val from = tabIndexOf(initialState.key)
                val to = tabIndexOf(targetState.key)
                when {
                    from == -1 || to == -1 || from == to -> SlideDirection.NONE
                    to > from -> SlideDirection.LEFT
                    else -> SlideDirection.RIGHT
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = { BackArrowButton(onClick = { outer.popBack() }) },
                actions = { TopicChip(topic = topic, modifier = Modifier.padding(end = 12.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                tabsOf().forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.route,
                        onClick = { model.select(tab.route) },
                        icon = { Text(text = tab.glyph, style = MaterialTheme.typography.titleLarge) },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        },
    ) { insets ->
        // Consumed so a tab's own Scaffold does not pad for the status bar a second time.
        Column(modifier = Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
            // A second host, mounted inside one entry of the root host. It provides its own
            // `LocalNavigator`, so a screen below calls `push` and lands in *this* stack — the
            // same screen would drive the root host if it were mounted there instead.
            NavigationHost(
                params = NavigationHostParams(
                    backStack = backStack,
                    onBackStackChange = model::onBackStackChange,
                    transitionSpec = tabTransition,
                    popTransitionSpec = tabTransition,
                ),
            ) {
                navEntry<HomeTab> { HomeTabBody() }
                navEntry<SearchTab> { SearchTabBody() }
                navEntry<SettingsTab> { SettingsTabBody() }

                // Nothing here pushes SignInRoute — the app-wide guard substitutes it when it
                // refuses the settings tab. A nested host that did not register it would hand
                // it a key it has no entry for, and the host throws.
                navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session, isEmbedded = true) }
            }
        }
    }
}

@Composable
private fun HomeTabBody() {
    TabPage(
        howItWorks = {
            Explanation(
                "The bar is a second `NavigationHost`, mounted inside one entry of the root host, and " +
                    "the tabs *are* its stack — one entry each. Selecting a tab replaces that stack; " +
                    "anything deeper would be pushed onto the host above the bar.",
            )
            Explanation(
                "Switching slides towards the tab you picked: the host's `transitionSpec` is " +
                    "`NavAnimations.slideTo`, which compares the positions of the two tabs.",
            )
        },
    ) {
        HeroCard(
            title = "Good morning, Ada",
            emoji = "☀️",
            accent = topic.accent,
            subtitle = "Three things happened while you were away.",
        )
        SectionLabel(text = "Today")
        ListRow(title = "Maya shared a board", emoji = "📌", accent = topic.accent, subtitle = "Spring campaign")
        ListRow(title = "Your weekly summary", emoji = "📊", accent = topic.accent, subtitle = "12 tasks done")
        ListRow(title = "New comment", emoji = "💬", accent = topic.accent, subtitle = "\"Looks great!\"")
    }
}

@Composable
private fun SearchTabBody() {
    var query by rememberSaveable { mutableStateOf("") }
    TabPage(
        howItWorks = {
            Explanation(
                "Type something, switch tabs and come back: the query is gone. A tab switch pops " +
                    "this entry, and a pop clears the entry's `rememberSaveable` state — the Settings " +
                    "tab shows where state that must survive a switch has to live.",
            )
        },
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { entered -> query = entered },
            placeholder = { Text(text = "Search boards, people, files") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel(text = "Recent")
        ListRow(title = "Spring campaign", emoji = "🕘", accent = topic.accent)
        ListRow(title = "Maya Chen", emoji = "🕘", accent = topic.accent)
        ListRow(title = "Q3 roadmap.pdf", emoji = "🕘", accent = topic.accent)
    }
}

@Composable
private fun SettingsTabBody() {
    // Scoped to the owner the host was mounted in rather than to this entry, so it outlives a tab
    // switch. `key` keeps each tab's state its own when several of them scope to the same owner.
    val model: SettingsTabViewModel = viewModel(
        viewModelStoreOwner = checkNotNull(LocalHostViewModelStoreOwner.current),
        key = "settings",
    ) {
        SettingsTabViewModel()
    }
    val edits by model.edits.collectAsState()

    TabPage(
        howItWorks = {
            LiveValue(label = "Unsaved changes", value = edits.toString())
            Explanation(
                "Make a few changes, switch to Home and come back: the count is still there. A tab " +
                    "switch replaces this host's stack, so this entry was popped and an entry-scoped " +
                    "ViewModel would have been cleared. This one hangs off the screen that mounts " +
                    "the tabs — `LocalHostViewModelStoreOwner` — which is the lifetime a tab actually has.",
            )
            Explanation(
                "This tab is also protected: its route carries the marker the application's " +
                    "sign-in guard narrows to, so selecting it signed out puts a sign-in screen here " +
                    "instead — inside this host, not the application's.",
            )
        },
    ) {
        ContentCard {
            ListRow(title = "Account", emoji = "👤", accent = topic.accent, subtitle = "ada@example.com")
            ListRow(title = "Notifications", emoji = "🔔", accent = topic.accent, subtitle = "Mentions and replies")
            ListRow(title = "Privacy", emoji = "🔒", accent = topic.accent, subtitle = "Only people you follow")
        }
        SecondaryButton(
            label = if (edits == 0) {
                "Change a setting"
            } else {
                "Change a setting · $edits unsaved"
            },
            onClick = model::edit,
        )
    }
}

/** A tab's scrollable page, with the example's explanation at the bottom. */
@Composable
private fun TabPage(
    howItWorks: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
        HowItWorks(topic = topic, content = howItWorks)
    }
}

private data class TabItem(
    val route: SingleHostTab,
    val label: String,
    val glyph: String,
)

/**
 * A scene's key is its entry's content key — the route's `toString()`. The guard's sign-in screen
 * stands in the position of the tab it guards, so moving to it slides like moving to that tab.
 */
private fun tabIndexOf(sceneKey: Any): Int {
    val key = sceneKey.toString()
    return tabsOf().indexOfFirst { tab ->
        key == tab.route.toString() || key == SignInRoute(next = tab.route).toString()
    }
}

private fun tabsOf(): List<TabItem> {
    return listOf(
        TabItem(route = HomeTab, label = "Home", glyph = "🏠"),
        TabItem(route = SearchTab, label = "Search", glyph = "🔍"),
        TabItem(route = SettingsTab, label = "Settings", glyph = "⚙️"),
    )
}
