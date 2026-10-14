package io.thernal.navkit.navigation.testing

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator

internal class TestBackStack(initial: List<Route>) {
    val routes: MutableList<Route> = initial.toMutableList()

    private val runner = NavigationGuardRunnerImpl(guards = emptyList())

    val navigator: Navigator = BackStackNavigator(
        buildBackStack = { builder -> routes.builder() },
        resolveCanPop = { routes.size > 1 },
        resolveGuardRunner = { runner },
        backDispatcher = BackDispatcherImpl(),
    )
}
