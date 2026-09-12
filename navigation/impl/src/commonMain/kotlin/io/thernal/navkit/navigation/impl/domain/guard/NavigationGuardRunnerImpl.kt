package io.thernal.navkit.navigation.impl.domain.guard

import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route

/** Runs every guard against every route and stops at the first one that does not allow it. */
class NavigationGuardRunnerImpl(private val guards: List<NavigationGuard>) : NavigationGuardRunner {
    override fun evaluate(route: Route): GuardResult {
        guards.forEach { guard ->
            when (val result = guard.evaluate(route)) {
                GuardResult.Allow -> Unit
                is GuardResult.Block -> return result
                is GuardResult.Redirect -> return result
            }
        }
        return GuardResult.Allow
    }
}
