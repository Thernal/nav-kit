package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEvent
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.api.presentation.navigator.pop
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackStackNavigatorTest {
    private data object Root : Route
    private data object Details : Route
    private data object Edit : Route
    private data object SignIn : Route

    /** Mirrors how `NavigationView` wires a navigator over an external, caller-owned back stack. */
    private class Harness(
        initial: List<Route>,
        guardRunner: NavigationGuardRunner = NavigationGuardRunnerImpl(emptyList()),
    ) {
        var backStack: List<Route> = initial
            private set

        val events = mutableListOf<NavigationEvent>()

        val navigator: Navigator = BackStackNavigator(
            buildBackStack = { builder -> backStack = backStack.toMutableList().apply(builder) },
            resolveCanPop = { backStack.size > 1 },
            guardRunner = guardRunner,
            events = NavigationEventSink { event -> events += event },
        )
    }

    @Test
    fun rootCannotBePopped() {
        val harness = Harness(listOf(Root))

        assertFalse(harness.navigator.pop())
        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun pushAndPopUpdateStack() {
        val harness = Harness(listOf(Root))

        harness.navigator.push(Details)
        assertEquals(listOf(Root, Details), harness.backStack)

        assertTrue(harness.navigator.pop())
        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun navigateReusesMatchingDestinationAndReplaceAllIsAtomic() {
        val harness = Harness(listOf(Root))
        harness.navigator.push(Details)
        harness.navigator.push(Edit)

        harness.navigator.navigate(Details)
        assertEquals(listOf(Root, Details), harness.backStack)

        harness.navigator.replaceAll(listOf(Root, Edit))
        assertEquals(listOf(Root, Edit), harness.backStack)
    }

    @Test
    fun pushDoesNotTouchStackWhenGuardBlocks() {
        val guard = NavigationGuard { GuardResult.Block("denied") }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.push(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun navigateDoesNotTouchStackWhenGuardBlocks() {
        val guard = NavigationGuard { GuardResult.Block("denied") }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.navigate(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun replaceDoesNotTouchStackWhenGuardBlocks() {
        val guard = NavigationGuard { GuardResult.Block("denied") }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.replace(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun replaceAllDoesNotTouchStackWhenItsLastRouteIsBlocked() {
        val guard = NavigationGuard { route ->
            if (route == Edit) {
                GuardResult.Block("denied")
            } else {
                GuardResult.Allow
            }
        }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.replaceAll(listOf(Root, Details, Edit))

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun pushSubstitutesTheRedirectTarget() {
        val guard = NavigationGuard { route ->
            if (route == Details) {
                GuardResult.Redirect(SignIn)
            } else {
                GuardResult.Allow
            }
        }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.push(Details)

        assertEquals(listOf(Root, SignIn), harness.backStack)
    }

    @Test
    fun replaceAllSubstitutesTheRedirectTargetForItsLastRouteOnly() {
        val guard = NavigationGuard { route ->
            if (route == Edit) {
                GuardResult.Redirect(SignIn)
            } else {
                GuardResult.Allow
            }
        }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.replaceAll(listOf(Root, Details, Edit))

        assertEquals(listOf(Root, Details, SignIn), harness.backStack)
    }

    @Test
    fun aBlockedRouteIsReportedToTheEventSink() {
        val guard = NavigationGuard { GuardResult.Block("denied") }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.push(Details)

        assertEquals<List<NavigationEvent>>(listOf(NavigationEvent.Blocked(Details, "denied")), harness.events)
    }

    @Test
    fun aPushIsReportedToTheEventSink() {
        val harness = Harness(listOf(Root))

        harness.navigator.push(Details)

        assertEquals<List<NavigationEvent>>(listOf(NavigationEvent.Push(Root, Details)), harness.events)
    }
}
