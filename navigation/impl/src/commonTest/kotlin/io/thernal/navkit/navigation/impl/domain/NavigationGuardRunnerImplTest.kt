package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.RouteGuard
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.assertNull

class NavigationGuardRunnerImplTest {
    private data object Root : Route
    private data object Details : Route
    private data object Edit : Route
    private data object SignIn : Route
    private data object Paywall : Route

    private data class Denied(override val message: String) : BlockReason

    private interface AuthGuarded : Route
    private data object Secret : AuthGuarded
    private data object AlsoSecret : AuthGuarded

    /** Refuses any proposal that contains [route], by handing the previous stack back. */
    private fun rejecting(
        route: Route,
        reason: BlockReason? = null,
    ): NavigationGuard {
        return NavigationGuard { old, new ->
            if (new.contains(route)) {
                GuardVerdict.Resolved(old, reason)
            } else {
                GuardVerdict.Resolved(new)
            }
        }
    }

    /** Rewrites [from] into [to] wherever it appears, the shape of a redirect. */
    private fun redirecting(
        from: Route,
        to: Route,
    ): NavigationGuard {
        return NavigationGuard { _, new ->
            if (new.contains(from)) {
                val rewritten = new.map { route ->
                    if (route == from) {
                        to
                    } else {
                        route
                    }
                }
                GuardVerdict.Resolved(rewritten.toImmutableList())
            } else {
                GuardVerdict.Resolved(new)
            }
        }
    }

    @Test
    fun allowsWhenThereAreNoGuards() {
        val runner = NavigationGuardRunnerImpl(emptyList())

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(stackOf(Root, Details), verdict.stack)
        assertNull(verdict.reason)
    }

    @Test
    fun allowsWhenEveryGuardAllows() {
        val runner = NavigationGuardRunnerImpl(listOf(rejecting(Paywall), rejecting(SignIn)))

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(stackOf(Root, Details), verdict.stack)
    }

    @Test
    fun aRefusalIsThePreviousStackComingBack() {
        val runner = NavigationGuardRunnerImpl(listOf(rejecting(Details, Denied("denied"))))

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(stackOf(Root), verdict.stack)
        assertEquals(Denied("denied"), verdict.reason)
    }

    @Test
    fun everyRouteOfAProposedStackIsGuardedNotOnlyItsLast() {
        val runner = NavigationGuardRunnerImpl(listOf(rejecting(Details)))

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details, Edit))

        assertEquals(stackOf(Root), verdict.stack)
    }

    @Test
    fun aRouteAGuardIntroducedIsItselfGuarded() {
        // The per-route runner pushed a redirect target exactly as named, unseen by every guard
        // including the one that produced it. Here the rewrite is simply the next proposal.
        val runner = NavigationGuardRunnerImpl(
            listOf(
                redirecting(from = Details, to = SignIn),
                rejecting(SignIn, Denied("sign-in is guarded too")),
            ),
        )

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(stackOf(Root), verdict.stack)
        assertEquals(Denied("sign-in is guarded too"), verdict.reason)
    }

    @Test
    fun aGuardSeesThePreviousGuardsOutput() {
        val runner = NavigationGuardRunnerImpl(
            listOf(
                redirecting(from = Details, to = SignIn),
                redirecting(from = SignIn, to = Paywall),
            ),
        )

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(stackOf(Root, Paywall), verdict.stack)
    }

    @Test
    fun aGuardCanRefuseAPop() {
        // Only expressible because a guard sees the transition: nothing is entering here.
        val guard = NavigationGuard { old, new ->
            if (old.contains(Edit) && !new.contains(Edit)) {
                GuardVerdict.Resolved(old, Denied("unsaved changes"))
            } else {
                GuardVerdict.Resolved(new)
            }
        }
        val runner = NavigationGuardRunnerImpl(listOf(guard))

        val verdict = runner.resolve(old = stackOf(Root, Edit), new = stackOf(Root))

        assertEquals(stackOf(Root, Edit), verdict.stack)
        assertEquals(Denied("unsaved changes"), verdict.reason)
    }

    @Test
    fun revalidationPassesTheUnmovedStackAsBothArguments() {
        val runner = NavigationGuardRunnerImpl(listOf(authGuard()))

        // Nothing moved; the guards may simply answer differently than they did on the way in.
        val verdict = runner.resolve(old = stackOf(Root, Secret), new = stackOf(Root, Secret))

        assertEquals(stackOf(Root, SignIn), verdict.stack)
    }

    @Test
    fun aStackThatIsItsOwnPreviousIsStillJudgedRouteByRoute() {
        // The shape the host uses on its first composition, which is the path a cold-start deep
        // link takes: nothing is in effect yet, so the stack handed in is also `old`. A transition
        // guard correctly finds nothing to do; a route-scoped guard still judges every route it
        // owns. Passing an empty `old` here instead would have the transition guard hand back an
        // empty stack, which is a refusal the runner cannot apply.
        val unsavedWorkGuard = NavigationGuard { old, new ->
            if (old.contains(Edit) && !new.contains(Edit)) {
                GuardVerdict.Resolved(old, Denied("unsaved changes"))
            } else {
                GuardVerdict.Resolved(new)
            }
        }
        val runner = NavigationGuardRunnerImpl(listOf(unsavedWorkGuard, authGuard()))

        val verdict = runner.resolve(old = stackOf(Root, Secret), new = stackOf(Root, Secret))

        assertEquals(stackOf(Root, SignIn), verdict.stack)
    }

    @Test
    fun failsWhenAGuardEmptiesANonEmptyStack() {
        val guard = NavigationGuard { _, _ -> GuardVerdict.Resolved(persistentListOf()) }
        val runner = NavigationGuardRunnerImpl(listOf(guard))

        assertFailsWith<IllegalStateException> {
            runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))
        }
    }

    @Test
    fun failsWhenAGuardReordersTheRoutesItKept() {
        val guard = NavigationGuard { _, new -> GuardVerdict.Resolved(new.reversed().toImmutableList()) }
        val runner = NavigationGuardRunnerImpl(listOf(guard))

        assertFailsWith<IllegalStateException> {
            runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))
        }
    }

    @Test
    fun failsWhenGuardsDoNotSettle() {
        val runner = NavigationGuardRunnerImpl(
            listOf(
                redirecting(from = Details, to = SignIn),
                redirecting(from = SignIn, to = Details),
            ),
        )

        assertFailsWith<IllegalStateException> {
            runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))
        }
    }

    @Test
    fun routeGuardOnlyLooksAtTheRoutesItsMarkerNames() {
        val seen = mutableListOf<Route>()
        val runner = NavigationGuardRunnerImpl(listOf(authGuard(seen)))

        val untouched = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))
        val redirected = runner.resolve(old = stackOf(Root), new = stackOf(Root, Secret))

        assertEquals(stackOf(Root, Details), untouched.stack)
        assertEquals(stackOf(Root, SignIn), redirected.stack)
        assertEquals<List<Route>>(listOf(Secret), seen)
    }

    @Test
    fun routeGuardAlsoJudgesRoutesThatWereAlreadyOnTheStack() {
        // Not a diff: "Secret requires a session" holds for any stack containing Secret, however it
        // got there. Judging only what is entering is what would make a revalidation a no-op.
        val runner = NavigationGuardRunnerImpl(listOf(authGuard()))

        val verdict = runner.resolve(old = stackOf(Root, Secret), new = stackOf(Root, Secret, Details))

        assertEquals(stackOf(Root, SignIn, Details), verdict.stack)
    }

    @Test
    fun routeGuardCollapsesTwoRoutesRedirectedToTheSameDestination() {
        val runner = NavigationGuardRunnerImpl(listOf(authGuard()))

        val verdict = runner.resolve(old = stackOf(Root), new = stackOf(Root, Secret, AlsoSecret))

        assertEquals(stackOf(Root, SignIn), verdict.stack)
    }

    private fun authGuard(seen: MutableList<Route> = mutableListOf()): RouteGuard<AuthGuarded> {
        return object : RouteGuard<AuthGuarded>({ route -> route as? AuthGuarded }) {
            override val reason = Denied("sign in first")

            override fun redirect(
                route: AuthGuarded,
                stack: ImmutableList<Route>,
            ): Route {
                seen += route
                return SignIn
            }
        }
    }

    @Test
    fun extendedWithReturnsItselfWhenAHostContributesNothing() {
        val runner = NavigationGuardRunnerImpl(listOf(rejecting(Details)))

        assertSame(runner, runner.extendedWith(persistentListOf()))
    }

    @Test
    fun extendedWithAppliesBothTheAppWideAndTheHostsOwnGuards() {
        val appRunner = NavigationGuardRunnerImpl(listOf(rejecting(Paywall)))
        val hostRunner = appRunner.extendedWith(persistentListOf(rejecting(Details)))

        // The app-wide rule still holds, and the host's own is applied on the same stack.
        assertEquals(stackOf(Root, Details), appRunner.resolve(old = stackOf(Root), new = stackOf(Root, Details)).stack)
        assertEquals(stackOf(Root), hostRunner.resolve(old = stackOf(Root), new = stackOf(Root, Details)).stack)
        assertEquals(stackOf(Root), hostRunner.resolve(old = stackOf(Root), new = stackOf(Root, Paywall)).stack)
    }

    // The block body returns the TestResult rather than discarding it: on a JS target that value
    // is the only thing that keeps the runner waiting for the coroutine.
    @Test
    fun invalidationsCarryEveryGuardsStreamIncludingAHostsOwn(): TestResult {
        return runTest {
            val appRunner = NavigationGuardRunnerImpl(listOf(invalidatingGuard(times = 2)))
            val hostRunner = appRunner.extendedWith(persistentListOf(invalidatingGuard(times = 3)))

            assertEquals(2, appRunner.invalidations.toList().size)
            assertEquals(5, hostRunner.invalidations.toList().size)
        }
    }

    @Test
    fun invalidationsAreSilentForGuardsThatDoNotOverrideThem(): TestResult {
        return runTest {
            val runner = NavigationGuardRunnerImpl(listOf(rejecting(Details), authGuard()))

            assertEquals(emptyList(), runner.invalidations.toList())
        }
    }

    /** A guard that allows everything but announces [times] invalidations. */
    private fun invalidatingGuard(times: Int): NavigationGuard {
        return object : NavigationGuard {
            override val invalidations: Flow<Unit> = flowOf(*Array(times) { Unit })

            override fun evaluate(
                old: ImmutableList<Route>,
                new: ImmutableList<Route>,
            ): GuardVerdict {
                return GuardVerdict.Resolved(new)
            }
        }
    }

    @Test
    fun resolveDeferrableSurfacesADeferralAndResolveCollapsesItToWhatExistsMeanwhile() {
        val runner = NavigationGuardRunnerImpl(listOf(deferring(Details)))

        val deferrable = runner.resolveDeferrable(old = stackOf(Root), new = stackOf(Root, Details))
        val synchronous = runner.resolve(old = stackOf(Root), new = stackOf(Root, Details))

        // The synchronous fold is what composition and revalidation call, so it can never start
        // work: it takes the deferral at its `meanwhile` and drops it.
        assertTrue(deferrable is GuardVerdict.Deferred)
        assertEquals(stackOf(Root), deferrable.meanwhile)
        assertEquals(stackOf(Root), synchronous.stack)
    }

    @Test
    fun aDeferralStopsTheFoldSoLaterGuardsDoNotJudgeAStackNobodySettledOn() {
        var wasLaterGuardCalled = false
        val later = NavigationGuard { _, new ->
            wasLaterGuardCalled = true
            GuardVerdict.Resolved(new)
        }
        val runner = NavigationGuardRunnerImpl(listOf(deferring(Details), later))

        runner.resolveDeferrable(old = stackOf(Root), new = stackOf(Root, Details))

        assertEquals(false, wasLaterGuardCalled)
    }

    /** Defers whenever [route] is proposed, holding the stack where it was. */
    private fun deferring(route: Route): NavigationGuard {
        return NavigationGuard { old, new ->
            if (new.contains(route)) {
                GuardVerdict.Deferred(meanwhile = old) { GuardVerdict.Resolved(new) }
            } else {
                GuardVerdict.Resolved(new)
            }
        }
    }

    private fun stackOf(vararg routes: Route): ImmutableList<Route> {
        return routes.toList().toImmutableList()
    }
}
