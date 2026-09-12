package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.navkit.navigation.api.presentation.model.Route

val LocalNavigator = staticCompositionLocalOf<Navigator> { NoOpNavigator }

/**
 * Discards every command. A composable resolved above the `NavigationHost` that installs the real
 * [Navigator] (an app root, an isolated preview) reads [LocalNavigator] safely instead of crashing.
 */
private object NoOpNavigator : Navigator {
    override fun buildStack(builder: MutableList<Route>.() -> Unit) {
        // Intentionally empty: there is no stack above a host to build.
    }

    override fun canPop(): Boolean {
        return false
    }

    override fun push(route: Route) {
        // Intentionally empty: every command below is discarded for the same reason.
    }

    override fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)?,
    ) {
    }

    override fun replace(route: Route) {
    }

    override fun replaceAll(routes: List<Route>) {
    }

    override fun popBack(force: Boolean): Boolean {
        return false
    }

    override fun popBack(count: Int): Boolean {
        return false
    }

    override fun popBackTo(
        inclusive: Boolean,
        predicate: (Route) -> Boolean,
    ): Boolean {
        return false
    }
}
