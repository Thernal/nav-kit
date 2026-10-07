package io.thernal.navkit.sample.shared.tabs

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.guards.SessionStore

internal class TabsGraphProvider(private val session: SessionStore) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<SingleHostTabsRoute> { SingleHostTabsScreen(session) }
    }
}
