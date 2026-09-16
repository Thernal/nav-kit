package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEvent
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.api.presentation.navigator.pop
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackStackNavigatorTest {
    private data object Root : Route
    private data object Details : Route
    private data object Edit : Route
    private data object SignIn : Route

    private data class Denied(override val message: String) : BlockReason

    /** Refuses any proposal containing [route] by handing the previous stack back. */
    private fun rejecting(route: Route): NavigationGuard {
        return NavigationGuard { old, new ->
            if (new.contains(route)) {
                GuardVerdict.Resolved(old, Denied("denied"))
            } else {
                GuardVerdict.Resolved(new)
            }
        }
    }

    private fun redirecting(
        from: Route,
        to: Route,
    ): NavigationGuard {
        return NavigationGuard { _, new ->
            val rewritten = new.map { route ->
                if (route == from) {
                    to
                } else {
                    route
                }
            }
            GuardVerdict.Resolved(rewritten.toImmutableList())
        }
    }

    /** Mirrors how `NavigationHost` wires a navigator over an external, caller-owned back stack. */
    private class Harness(
        initial: List<Route>,
        guardRunner: NavigationGuardRunner = NavigationGuardRunnerImpl(emptyList()),
    ) {
        var backStack: List<Route> = initial
            private set

        val events = mutableListOf<NavigationEvent>()

        val backDispatcher: BackDispatcher = BackDispatcherImpl()

        val navigator: Navigator = BackStackNavigator(
            buildBackStack = { builder -> backStack = backStack.toMutableList().apply(builder) },
            resolveCanPop = { backStack.size > 1 },
            resolveGuardRunner = { guardRunner },
            backDispatcher = backDispatcher,
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
    fun pushDoesNotTouchStackWhenGuardRefuses() {
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.push(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun navigateDoesNotTouchStackWhenGuardRefuses() {
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.navigate(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun replaceDoesNotTouchStackWhenGuardRefuses() {
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.replace(Details)

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun replaceAllIsGuardedOnEveryRouteNotOnlyItsLast() {
        // The per-command runner resolved `routes.last()` alone, so a deep link's intermediate
        // routes reached the stack unguarded.
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.replaceAll(listOf(Root, Details, Edit))

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun buildStackIsGuardedLikeEveryOtherCommand() {
        // Previously the one public way into the stack that never consulted a guard.
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.buildStack { add(Details) }

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun aPopCanBeRefusedByAGuard() {
        val guard = NavigationGuard { old, new ->
            if (old.contains(Edit) && !new.contains(Edit)) {
                GuardVerdict.Resolved(old, Denied("unsaved changes"))
            } else {
                GuardVerdict.Resolved(new)
            }
        }
        val harness = Harness(listOf(Root, Edit), NavigationGuardRunnerImpl(listOf(guard)))

        harness.navigator.pop()

        assertEquals(listOf(Root, Edit), harness.backStack)
    }

    @Test
    fun pushSubstitutesTheRedirectTarget() {
        val harness = Harness(
            listOf(Root),
            NavigationGuardRunnerImpl(listOf(redirecting(from = Details, to = SignIn))),
        )

        harness.navigator.push(Details)

        assertEquals(listOf(Root, SignIn), harness.backStack)
    }

    @Test
    fun replaceAllSubstitutesARedirectTargetWhereverItSits() {
        val harness = Harness(
            listOf(Root),
            NavigationGuardRunnerImpl(listOf(redirecting(from = Details, to = SignIn))),
        )

        harness.navigator.replaceAll(listOf(Root, Details, Edit))

        assertEquals(listOf(Root, SignIn, Edit), harness.backStack)
    }

    @Test
    fun aRefusedStackIsReportedToTheEventSink() {
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))

        harness.navigator.push(Details)

        assertEquals<List<NavigationEvent>>(
            listOf(
                NavigationEvent.Blocked(
                    attempted = listOf(Root, Details).toImmutableList(),
                    applied = listOf<Route>(Root).toImmutableList(),
                    reason = Denied("denied"),
                ),
            ),
            harness.events,
        )
    }

    @Test
    fun aRewrittenStackIsNotReportedAsThePushThatWasAskedFor() {
        val harness = Harness(
            listOf(Root),
            NavigationGuardRunnerImpl(listOf(redirecting(from = Details, to = SignIn))),
        )

        harness.navigator.push(Details)

        assertEquals<List<NavigationEvent>>(
            listOf(
                NavigationEvent.Blocked(
                    attempted = listOf(Root, Details).toImmutableList(),
                    applied = listOf<Route>(Root, SignIn).toImmutableList(),
                    reason = null,
                ),
            ),
            harness.events,
        )
    }

    @Test
    fun aProgrammaticPopGoesThroughTheSameBackDispatcherAsTheGesture() {
        // Previously only the host's gesture consulted the dispatcher, so an in-app back button
        // calling pop() bypassed every screen that had registered an interceptor.
        val harness = Harness(listOf(Root, Edit))
        var didIntercept = false
        harness.backDispatcher.register(
            BackCallback {
                didIntercept = true
                true
            },
        )

        assertTrue(harness.navigator.pop())

        assertTrue(didIntercept)
        assertEquals(listOf(Root, Edit), harness.backStack)
    }

    @Test
    fun popBackToIsAJumpAndDoesNotConsultInterceptors() {
        val harness = Harness(listOf(Root, Details, Edit))
        harness.backDispatcher.register(BackCallback { true })

        assertTrue(harness.navigator.popBackTo { route -> route == Root })

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun anInterceptorThatPopsFromInsideItsOwnHandlerIsNotDispatchedToAgain() {
        val harness = Harness(listOf(Root, Edit))
        harness.backDispatcher.register(
            BackCallback {
                harness.navigator.pop()
                true
            },
        )

        harness.navigator.pop()

        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun aCommandReportsWhatActuallyHappenedToItsCaller() {
        val applied = Harness(listOf(Root)).navigator.push(Details)
        val rewritten = Harness(
            listOf(Root),
            NavigationGuardRunnerImpl(listOf(redirecting(from = Details, to = SignIn))),
        ).navigator.push(Details)
        val refused = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(rejecting(Details))))
            .navigator.push(Details)

        assertEquals(NavigationOutcome.Applied(listOf<Route>(Root, Details).toImmutableList()), applied)
        assertEquals(
            NavigationOutcome.Rewritten(listOf<Route>(Root, SignIn).toImmutableList(), null),
            rewritten,
        )
        assertEquals(
            NavigationOutcome.Rewritten(listOf<Route>(Root).toImmutableList(), Denied("denied")),
            refused,
        )
    }

    @Test
    fun aDeferredCommandSaysSoAndShowsWhatExistsMeanwhile() {
        val guard = NavigationGuard { old, new ->
            if (new.contains(Details)) {
                GuardVerdict.Deferred(meanwhile = old) { GuardVerdict.Resolved(new) }
            } else {
                GuardVerdict.Resolved(new)
            }
        }
        val harness = Harness(listOf(Root), NavigationGuardRunnerImpl(listOf(guard)))

        val outcome = harness.navigator.push(Details)

        assertEquals(NavigationOutcome.Deferred(listOf<Route>(Root).toImmutableList()), outcome)
        assertEquals(listOf(Root), harness.backStack)
    }

    @Test
    fun aPushIsReportedToTheEventSink() {
        val harness = Harness(listOf(Root))

        harness.navigator.push(Details)

        assertEquals<List<NavigationEvent>>(listOf(NavigationEvent.Push(Root, Details)), harness.events)
    }
}
