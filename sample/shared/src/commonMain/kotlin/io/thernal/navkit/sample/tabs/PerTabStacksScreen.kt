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
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.guards.SessionStore
import io.thernal.navkit.sample.guards.SignInRoute
import io.thernal.navkit.sample.guards.SignInScreen
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote

@Composable
fun PerTabStacksScreen(session: SessionStore) {
    val model: PerTabStacksViewModel = viewModel { PerTabStacksViewModel() }
    val tab by model.current.collectAsState()
    val stacks by model.backStack.collectAsState()
    val stack = stacks.getValue(tab)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = entry == tab,
                        onClick = { model.select(entry) },
                        icon = { Text(text = entry.label.take(1)) },
                        label = { Text(text = entry.label) },
                    )
                }
            }
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            NavigationHost(
                params = NavigationHostParams(
                    backStack = stack,
                    onBackStackChange = model::onBackStackChange,
                ),
            ) {
                navEntry<FeedList> { ListBody(title = "Feed", open = { id -> FeedItem(id) }) }
                navEntry<FeedItem> { route -> DetailBody(title = "Feed item ${route.id}") }
                navEntry<SavedList> { ListBody(title = "Saved", open = { id -> SavedItem(id) }) }
                navEntry<SavedItem> { route -> DetailBody(title = "Saved item ${route.id}") }
                navEntry<AdminDashboard> { DetailBody(title = "Admin dashboard") }

                // The application-wide guard applies to this host's stack too, and when it refuses
                // the admin tab it *substitutes* a sign-in route. A nested host that did not
                // register that route would hand Navigation3 a key it has no entry for, and the
                // default fallback throws. Registering the guard's destination is part of using a
                // guard in a nested host.
                navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session) }
            }
        }
    }
}

private val ITEM_IDS = listOf(1, 2, 3)

@Composable
private fun ListBody(
    title: String,
    open: (Int) -> Route,
) {
    val navigator = LocalNavigator.current
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        ITEM_IDS.forEach { id ->
            ExampleAction(
                label = "Open $id",
                onClick = { navigator.push(open(id)) },
            )
        }
        ExampleNote(
            text = "Go deep here, switch tabs, and come back: the depth is still there. The tab " +
                "you left kept its own list, because the stacks belong to the screen's ViewModel " +
                "and only one of them is handed to the host at a time.",
        )
    }
}

@Composable
private fun DetailBody(title: String) {
    val navigator = LocalNavigator.current
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        ExampleAction(
            label = "Back",
            onClick = { navigator.popBack() },
            enabled = navigator.canPop(),
        )
        ExampleNote(
            text = "The Admin tab is a protected graph: its routes carry the same marker the " +
                "application's sign-in guard narrows to, so selecting it signed out puts a " +
                "sign-in screen there instead — inside this host, not the application's.",
        )
    }
}
