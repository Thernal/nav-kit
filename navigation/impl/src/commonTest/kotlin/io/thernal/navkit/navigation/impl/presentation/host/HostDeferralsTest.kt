package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val RIGHT_PIN = "1234"
private const val FRAMES_TO_SETTLE = 6

class HostDeferralsTest {
    private data object Lobby : Route
    private data object Vault : Route
    private data object PinPrompt : Route
    private data object Elsewhere : Route

    /** The sample's PIN session, reduced to what the guard reads. */
    private class PinSession {
        var isLocked = false
        private val submissions = MutableSharedFlow<String>(extraBufferCapacity = 1)

        fun submit(pin: String) {
            submissions.tryEmit(pin)
        }

        suspend fun awaitUnlock(): Boolean {
            if (submissions.first() == RIGHT_PIN) {
                isLocked = false
            }
            return !isLocked
        }
    }

    /** Defers any stack holding [Vault] while locked, showing everything but the vault meanwhile. */
    private class PinGuard(private val session: PinSession) : NavigationGuard {
        override fun evaluate(
            old: ImmutableList<Route>,
            new: ImmutableList<Route>,
        ): GuardVerdict {
            if (!session.isLocked || Vault !in new) {
                return GuardVerdict.Resolved(new)
            }
            val locked = old.filterNot { route -> route == Vault }.toImmutableList()
            return GuardVerdict.Deferred(meanwhile = locked) { navigator ->
                navigator.push(PinPrompt)
                if (session.awaitUnlock()) {
                    GuardVerdict.Resolved(new)
                } else {
                    GuardVerdict.Resolved(locked)
                }
            }
        }
    }

    /**
     * `NavigationHostImpl` without Compose: the same writer, slot and pair of navigators, and a
     * [frame] that performs the host's composition and effects in the order the host does.
     */
    private class Host(
        initial: List<Route>,
        session: PinSession,
    ) {
        var owned: ImmutableList<Route> = initial.toImmutableList()

        private val runner = NavigationGuardRunnerImpl(listOf(PinGuard(session)))
        private val writer = HostStackWriter<Route> { stack -> owned = stack }
        val deferrals = HostDeferrals()
        private var rendered: ImmutableList<Route>? = null
        private var driven: PendingDeferral? = null
        private var drive: Job? = null

        val screens: Navigator = navigator(isForDeferral = false)
        private val deferral: Navigator = navigator(isForDeferral = true)

        private fun navigator(isForDeferral: Boolean): Navigator {
            return BackStackNavigator(
                buildBackStack = { builder ->
                    val mutable = writer.newestOr(checkNotNull(rendered)).toMutableList()
                    mutable.builder()
                    writer.write(mutable.toImmutableList())
                },
                resolveCanPop = { checkNotNull(rendered).size > 1 },
                resolveGuardRunner = { runner },
                backDispatcher = BackDispatcherImpl(),
                onDeferred = if (isForDeferral) {
                    { _, _ -> }
                } else {
                    deferrals::submit
                },
                onMoved = if (isForDeferral) {
                    {}
                } else {
                    deferrals::abandon
                },
            )
        }

        fun frame(scope: CoroutineScope) {
            // Composition: resolve what was handed in, then the host's SideEffect.
            val handed = owned
            val verdict = runner.resolveDeferrable(old = rendered ?: handed, new = handed)
            val resolved = when (verdict) {
                is GuardVerdict.Resolved -> verdict.stack
                is GuardVerdict.Deferred -> verdict.meanwhile
            }
            val pending = deferrals.pending
            rendered = resolved
            if (writer.acknowledge(handed)) {
                deferrals.abandon()
            }
            // Effects: write-back, the host's own submission, then the run keyed on `pending`.
            if (resolved != handed) {
                writer.write(resolved)
            }
            if (verdict is GuardVerdict.Deferred) {
                deferrals.submit(attempted = handed, deferral = verdict)
            }
            if (pending !== driven) {
                drive?.cancel()
                driven = pending
                drive = pending?.let { run -> scope.launch { deferrals.drive(run = run, navigator = deferral) } }
            }
        }
    }

    private fun TestScope.settle(host: Host) {
        repeat(FRAMES_TO_SETTLE) {
            host.frame(backgroundScope)
            runCurrent()
        }
    }

    @Test
    fun aLockedPushAsksForThePinAndContinuesOnceItIsRight(): TestResult {
        // The navigator used to write `meanwhile` and drop the deferral, so the push did nothing.
        return runTest {
            val session = PinSession()
            val host = Host(listOf(Lobby), session)
            settle(host)
            session.isLocked = true

            val outcome = host.screens.push(Vault)
            settle(host)
            assertIs<NavigationOutcome.Deferred>(outcome)
            assertEquals(listOf<Route>(Lobby, PinPrompt), host.owned)

            session.submit(RIGHT_PIN)
            settle(host)
            assertEquals(listOf<Route>(Lobby, Vault), host.owned)
            assertNull(host.deferrals.pending)
        }
    }

    @Test
    fun aLockInsideTheVaultAsksForThePinAndReturnsToTheVault(): TestResult {
        // The run was keyed on the verdict, and its own prompt push changed the verdict.
        return runTest {
            val session = PinSession()
            val host = Host(listOf(Lobby, Vault), session)
            settle(host)

            session.isLocked = true
            settle(host)
            assertEquals(listOf<Route>(Lobby, PinPrompt), host.owned)

            session.submit(RIGHT_PIN)
            settle(host)
            assertEquals(listOf<Route>(Lobby, Vault), host.owned)
        }
    }

    @Test
    fun aWrongPinLeavesTheVaultShut(): TestResult {
        return runTest {
            val session = PinSession()
            val host = Host(listOf(Lobby), session)
            settle(host)
            session.isLocked = true

            host.screens.push(Vault)
            settle(host)
            session.submit("0000")
            settle(host)

            assertEquals(listOf<Route>(Lobby), host.owned)
            assertTrue(session.isLocked)
        }
    }

    @Test
    fun backingOutOfThePromptAbandonsTheWaitAndAnotherAttemptAsksAgain(): TestResult {
        return runTest {
            val session = PinSession()
            val host = Host(listOf(Lobby), session)
            settle(host)
            session.isLocked = true
            host.screens.push(Vault)
            settle(host)

            host.screens.popBack()
            settle(host)
            assertNull(host.deferrals.pending)
            session.submit(RIGHT_PIN)
            settle(host)
            assertEquals(listOf<Route>(Lobby), host.owned)

            host.screens.push(Vault)
            settle(host)
            assertEquals(listOf<Route>(Lobby, PinPrompt), host.owned)
            session.submit(RIGHT_PIN)
            settle(host)
            assertEquals(listOf<Route>(Lobby, Vault), host.owned)
        }
    }

    @Test
    fun aStackArrivingFromElsewhereAbandonsTheWait(): TestResult {
        return runTest {
            val session = PinSession()
            val host = Host(listOf(Lobby), session)
            settle(host)
            session.isLocked = true
            host.screens.push(Vault)
            settle(host)

            host.owned = persistentListOf(Elsewhere)
            settle(host)
            session.submit(RIGHT_PIN)
            settle(host)

            assertEquals(listOf<Route>(Elsewhere), host.owned)
            assertNull(host.deferrals.pending)
        }
    }

    @Test
    fun aSecondSubmissionForTheSameAttemptKeepsTheRunningOne() {
        val deferrals = HostDeferrals()
        val attempted = persistentListOf<Route>(Lobby, Vault)
        val waitForever = GuardVerdict.Deferred(meanwhile = persistentListOf(Lobby)) {
            GuardVerdict.Resolved(attempted)
        }

        deferrals.submit(attempted = attempted, deferral = waitForever)
        val first = deferrals.pending
        deferrals.submit(attempted = attempted, deferral = waitForever.copy())
        assertSame(first, deferrals.pending)

        deferrals.submit(attempted = persistentListOf(Vault), deferral = waitForever)
        assertTrue(first !== deferrals.pending)
    }

    @Test
    fun aRunAbandonedWhileFinishingIsNotApplied(): TestResult {
        return runTest {
            val deferrals = HostDeferrals()
            val answer = CompletableDeferred<GuardVerdict>()
            var applied: List<Route>? = null
            val navigator = BackStackNavigator(
                buildBackStack = { builder -> applied = mutableListOf<Route>(Lobby).apply(builder) },
                resolveCanPop = { false },
                resolveGuardRunner = { NavigationGuardRunnerImpl(emptyList()) },
                backDispatcher = BackDispatcherImpl(),
            )
            deferrals.submit(
                attempted = persistentListOf(Lobby, Vault),
                deferral = GuardVerdict.Deferred(meanwhile = persistentListOf(Lobby)) { answer.await() },
            )
            val run = checkNotNull(deferrals.pending)
            backgroundScope.launch { deferrals.drive(run = run, navigator = navigator) }
            runCurrent()

            deferrals.abandon()
            answer.complete(GuardVerdict.Resolved(persistentListOf(Lobby, Vault)))
            runCurrent()

            assertNull(applied)
        }
    }
}
