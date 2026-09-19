package io.thernal.navkit.sample.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.navkit.navigation.api.presentation.host.LocalHostViewModelStoreOwner
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.sample.guards.SessionStore
import io.thernal.navkit.sample.guards.SignInRoute
import io.thernal.navkit.sample.guards.SignInScreen
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout

@Composable
fun SingleHostTabsScreen(session: SessionStore) {
    val model: SingleHostTabsViewModel = viewModel { SingleHostTabsViewModel() }
    val backStack by model.backStack.collectAsState()
    val selected = backStack.first()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabsOf().forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab.route,
                        onClick = { model.select(tab.route) },
                        icon = { Text(text = tab.glyph) },
                        label = { Text(text = tab.label) },
                    )
                }
            }
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            // A second host, mounted inside one entry of the root host. It provides its own
            // `LocalNavigator`, so a screen below calls `push` and lands in *this* stack — the
            // same screen would drive the root host if it were mounted there instead.
            NavigationHost(
                params = NavigationHostParams(
                    backStack = backStack,
                    onBackStackChange = model::onBackStackChange,
                ),
            ) {
                navEntry<HomeTab> { TabBody(title = "Home", body = "Nothing to see, that is the point.") }
                navEntry<SearchTab> { TabBody(title = "Search", body = "Still one stack, still one host.") }
                navEntry<SettingsTab> { SettingsTabBody() }

                // Nothing here pushes SignInRoute — the app-wide guard substitutes it when it
                // refuses the settings tab. A nested host that did not register it would hand
                // Navigation3 a key it has no entry for, and the default fallback throws.
                navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session) }
            }
        }
    }
}

@Composable
private fun TabBody(
    title: String,
    body: String,
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
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

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        ExampleReadout(label = "Unsaved edits", value = edits.toString())
        ExampleAction(
            label = "Make an edit",
            onClick = model::edit,
        )
        ExampleNote(
            text = "Make a few edits, switch to Home and come back: the count is still there. A " +
                "tab switch replaces this host's stack, so this entry was popped and an " +
                "entry-scoped ViewModel would have been cleared. This one hangs off the screen " +
                "that mounts the tabs, which is the lifetime a tab actually has — leave the " +
                "example and the count goes with it.",
        )
        ExampleNote(
            text = "This tab is also protected: its route carries the marker the application's " +
                "sign-in guard narrows to, so selecting it signed out puts a sign-in screen here " +
                "instead — inside this host, not the application's.",
        )
    }
}

private data class TabItem(
    val route: SingleHostTab,
    val label: String,
    val glyph: String,
)

private fun tabsOf(): List<TabItem> {
    return listOf(
        TabItem(route = HomeTab, label = "Home", glyph = "H"),
        TabItem(route = SearchTab, label = "Search", glyph = "S"),
        TabItem(route = SettingsTab, label = "Settings", glyph = "⚙"),
    )
}
