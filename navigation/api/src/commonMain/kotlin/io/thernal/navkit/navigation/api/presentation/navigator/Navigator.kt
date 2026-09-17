package io.thernal.navkit.navigation.api.presentation.navigator

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Pure command surface, holding no back-stack state: `impl` builds one per mounted host. Commands
 * that add routes answer a [NavigationOutcome], because a guard makes it a real question whether it
 * happened; the pops answer a `Boolean`.
 */
interface Navigator {
    fun buildStack(builder: MutableList<Route>.() -> Unit): NavigationOutcome

    fun canPop(): Boolean

    fun push(route: Route): NavigationOutcome

    fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)? = null,
    ): NavigationOutcome

    fun replace(route: Route): NavigationOutcome

    fun replaceAll(route: Route): NavigationOutcome {
        return replaceAll(listOf(route))
    }

    fun replaceAll(routes: List<Route>): NavigationOutcome

    /**
     * Consults the host's `BackDispatcher` first, so a screen intercepting back is heard whichever
     * way back came. [popBackTo] deliberately does not: that is a jump, not a back.
     */
    fun popBack(force: Boolean = false): Boolean

    fun popBack(count: Int): Boolean

    /**
     * Returns whether the stack actually moved — `false` when nothing matches, when the match is
     * already on top, and when a guard refuses the jump.
     */
    fun popBackTo(
        inclusive: Boolean = false,
        predicate: (Route) -> Boolean,
    ): Boolean
}
