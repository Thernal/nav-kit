package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.persistentListOf

val LocalNavigator = staticCompositionLocalOf<Navigator> { NoOpNavigator }

/**
 * Discards every command, so a composable above any `NavigationHost` — an app root, a preview —
 * reads [LocalNavigator] safely. It answers [NavigationOutcome.Rewritten] rather than `Applied`:
 * a caller that checks its outcome should read "this did not happen".
 */
private object NoOpNavigator : Navigator {
    private val nothing = NavigationOutcome.Rewritten(stack = persistentListOf(), reason = null)

    override fun buildStack(builder: MutableList<Route>.() -> Unit): NavigationOutcome {
        return nothing
    }

    override fun canPop(): Boolean {
        return false
    }

    override fun push(route: Route): NavigationOutcome {
        return nothing
    }

    override fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)?,
    ): NavigationOutcome {
        return nothing
    }

    override fun replace(route: Route): NavigationOutcome {
        return nothing
    }

    override fun replaceAll(routes: List<Route>): NavigationOutcome {
        return nothing
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
