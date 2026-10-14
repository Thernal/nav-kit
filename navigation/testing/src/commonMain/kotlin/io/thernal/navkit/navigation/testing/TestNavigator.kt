package io.thernal.navkit.navigation.testing

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator

/** The kit's own navigator over a list, with no guards: a test runs commands on it and reads [stack]. */
class TestNavigator private constructor(
    private val backStack: TestBackStack,
) : Navigator by backStack.navigator {
    val stack: List<Route>
        get() = backStack.routes.toList()

    constructor(initial: List<Route> = emptyList()) : this(TestBackStack(initial))
}
