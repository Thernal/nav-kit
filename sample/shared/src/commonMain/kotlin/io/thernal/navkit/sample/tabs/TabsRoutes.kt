package io.thernal.navkit.sample.tabs

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.sample.app.SampleRoute
import io.thernal.navkit.sample.guards.AuthGuarded

sealed interface TabsRoute : SampleRoute

/** The outer route. Everything below lives inside the host this one screen mounts. */
data object SingleHostTabsRoute : TabsRoute

/**
 * The routes of the nested host: the tabs *are* its stack, one entry each.
 *
 * There is deliberately nothing to push onto a tab. A tab bar that also carries depth needs a stack
 * per tab, and a stack per tab over one host loses every hidden entry's state — so the shape that
 * stays honest is one entry per tab, with anything deeper pushed onto the host above.
 */
sealed interface SingleHostTab : Route

data object HomeTab : SingleHostTab

data object SearchTab : SingleHostTab

/**
 * The protected tab. Carrying the marker the application's `AuthGuard` already narrows to is the
 * whole of protecting it — a guard the app contributes applies to a nested host's stack exactly as
 * it does to the root's.
 */
data object SettingsTab : SingleHostTab, AuthGuarded
