package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route

interface NavigationGuardRunner {
    fun evaluate(route: Route): GuardResult
}
