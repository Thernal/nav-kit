package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationGuardRunnerImplTest {
    private data object Details : Route
    private data object SignIn : Route

    @Test
    fun allowsWhenThereAreNoGuards() {
        val runner = NavigationGuardRunnerImpl(emptyList())

        assertEquals(GuardResult.Allow, runner.evaluate(Details))
    }

    @Test
    fun allowsWhenEveryGuardAllows() {
        val runner = NavigationGuardRunnerImpl(
            listOf(
                NavigationGuard { GuardResult.Allow },
                NavigationGuard { GuardResult.Allow },
            ),
        )

        assertEquals(GuardResult.Allow, runner.evaluate(Details))
    }

    @Test
    fun shortCircuitsOnTheFirstNonAllowResult() {
        var wasSecondGuardCalled = false
        val runner = NavigationGuardRunnerImpl(
            listOf(
                NavigationGuard { GuardResult.Redirect(SignIn) },
                NavigationGuard {
                    wasSecondGuardCalled = true
                    GuardResult.Allow
                },
            ),
        )

        assertEquals(GuardResult.Redirect(SignIn), runner.evaluate(Details))
        assertEquals(false, wasSecondGuardCalled)
    }

    @Test
    fun returnsBlockFromTheGuardThatDenies() {
        val runner = NavigationGuardRunnerImpl(
            listOf(NavigationGuard { GuardResult.Block("denied") }),
        )

        assertEquals(GuardResult.Block("denied"), runner.evaluate(Details))
    }
}
