package io.thernal.navkit.sample.tabs

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
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote

@Composable
fun SingleHostTabsScreen() {
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
                navEntry<SettingsDetail> { route -> TabBody(title = route.section, body = "Pushed inside the tab.") }
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
    val navigator = LocalNavigator.current
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
        ExampleAction(
            label = "Open notifications",
            onClick = { navigator.push(SettingsDetail(section = "Notifications")) },
        )
        ExampleNote(
            text = "Pushing here goes onto the tab host's stack, not the application's. Switch " +
                "tabs and come back: the depth is gone, because selecting a tab replaced the " +
                "stack. Keeping it is the next example.",
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
