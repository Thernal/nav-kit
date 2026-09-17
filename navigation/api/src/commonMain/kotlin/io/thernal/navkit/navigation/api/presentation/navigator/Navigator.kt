package io.thernal.navkit.navigation.api.presentation.navigator

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Pure command surface — holds no back-stack state of its own. `impl` builds one per
 * [io.thernal.navkit.navigation.api.presentation.host.NavigationHost], over whatever backs that
 * host's `NavigationHostParams.backStack`.
 *
 * Every command that can add routes returns a [NavigationOutcome], because guards make it a real
 * question whether it happened. The pops return a `Boolean`, which is the whole question there.
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
     * Consults the host's [io.thernal.navkit.navigation.api.presentation.back.BackDispatcher] first,
     * so a screen intercepting back is heard whether back came from the gesture or from a button
     * calling this. [popBackTo] deliberately does not: that is a jump, not a back.
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
