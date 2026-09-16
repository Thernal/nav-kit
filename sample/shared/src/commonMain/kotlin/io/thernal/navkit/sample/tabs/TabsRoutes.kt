package io.thernal.navkit.sample.tabs

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.sample.app.SampleRoute
import io.thernal.navkit.sample.guards.AuthGuarded

sealed interface TabsRoute : SampleRoute

/** The outer route. Everything below lives inside the host this one screen mounts. */
data object SingleHostTabsRoute : TabsRoute

data object PerTabStacksRoute : TabsRoute

/** The routes of the single-host example: the tabs *are* the stack. */
sealed interface SingleHostTab : Route

data object HomeTab : SingleHostTab

data object SearchTab : SingleHostTab

data object SettingsTab : SingleHostTab

data class SettingsDetail(val section: String) : SingleHostTab

/** The routes of the per-tab example, one small graph per tab. */
sealed interface FeedRoute : Route

data object FeedList : FeedRoute

data class FeedItem(val id: Int) : FeedRoute

sealed interface SavedRoute : Route

data object SavedList : SavedRoute

data class SavedItem(val id: Int) : SavedRoute

/**
 * The protected tab. Marking its routes with the application's existing guard marker is the whole
 * of "protecting a graph" — the guard the app already contributes applies to a nested host's stack
 * exactly as it does to the root's.
 */
sealed interface AdminRoute : Route, AuthGuarded

data object AdminDashboard : AdminRoute

/** Which tab is showing. The stacks are held per entry of this enum. */
enum class Tab(val label: String) {
    FEED("Feed"),
    SAVED("Saved"),
    ADMIN("Admin"),
}
