package io.thernal.navkit.navigation.api.presentation.navigator

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Pure command surface — holds no back-stack state of its own. `impl` builds one per
 * [io.thernal.navkit.navigation.api.presentation.host.NavigationHost], adapting whatever backs
 * [io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams.backStack] (a
 * `StateFlow`-backed ViewModel state, typically) into these commands.
 */
interface Navigator {
    fun buildStack(builder: MutableList<Route>.() -> Unit)

    fun canPop(): Boolean

    fun push(route: Route)

    fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)? = null,
    )

    fun replace(route: Route)

    fun replaceAll(route: Route) {
        replaceAll(listOf(route))
    }

    fun replaceAll(routes: List<Route>)

    fun popBack(force: Boolean = false): Boolean

    fun popBack(count: Int): Boolean

    fun popBackTo(
        inclusive: Boolean = false,
        predicate: (Route) -> Boolean,
    ): Boolean
}
