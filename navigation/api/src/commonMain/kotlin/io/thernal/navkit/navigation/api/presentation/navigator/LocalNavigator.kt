package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.compose.runtime.staticCompositionLocalOf
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.persistentListOf

val LocalNavigator = staticCompositionLocalOf<Navigator> { NoOpNavigator }

/**
 * Discards every command. A composable resolved above the `NavigationHost` that installs the real
 * [Navigator] (an app root, an isolated preview) reads [LocalNavigator] safely instead of crashing.
 *
 * It reports [NavigationOutcome.Rewritten] over an empty stack rather than `Applied`, because a
 * caller that checks its outcome should read "this did not happen" — which is the truth here.
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
