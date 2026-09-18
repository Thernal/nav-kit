package io.thernal.navkit.sample.guards

import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * The guard that cannot answer yet.
 *
 * A 401 arrives while the user is somewhere protected. The rule is not "redirect them" — the
 * destination is still the right one, the session just needs re-proving. So the guard **defers**:
 * it says what may exist meanwhile, and hands back a suspending function that resolves once it
 * knows.
 *
 * Three things make this safe, and all three are the kit's doing rather than this guard's:
 *
 * - Only the mounted host awaits a deferral. It owns a scope tied to its own composition, so an
 *   unmounted host cancels what it started, and the same deferral is never launched twice however
 *   many times the host recomposes. Everything else — the navigator, a synchronous `resolve` — takes
 *   [GuardVerdict.Deferred.meanwhile] and starts nothing, so a revalidation storm is impossible by
 *   construction rather than by care.
 * - The placeholder is a `TransientRoute`, so a stack restored after process death cannot come back
 *   showing a PIN prompt that nothing is left to answer.
 * - Once the deferral settles, this guard answers synchronously from state that has already
 *   changed. A guard that defers again for the same stack simply never converges.
 */
class PinGuard(private val session: PinSession) : NavigationGuard {
    override val invalidations: Flow<Unit> = session.changes

    override fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict {
        if (!session.locked.value) {
            return GuardVerdict.Resolved(new)
        }
        if (new.none { route -> route is PinProtected }) {
            return GuardVerdict.Resolved(new)
        }
        return GuardVerdict.Deferred(meanwhile = lockedStack(old)) { navigator ->
            navigator.push(PinEntryRoute)
            if (session.awaitUnlock()) {
                // The stack the user originally asked for, which is the whole point of deferring
                // rather than redirecting: nothing about their intent was lost while we asked.
                GuardVerdict.Resolved(new)
            } else {
                GuardVerdict.Resolved(stack = lockedStack(old), reason = PinRequired)
            }
        }
    }

    /**
     * What may be on screen while the session is not trusted: everything except the protected
     * routes. A guard may drop routes, but it may not empty a non-empty stack — the runner rejects
     * a verdict that does, loudly — so a stack that is *only* protected routes stays as it is and
     * the PIN prompt goes on top of it.
     */
    private fun lockedStack(stack: ImmutableList<Route>): ImmutableList<Route> {
        val visible = stack.filterNot { route -> route is PinProtected }
        if (visible.isEmpty()) {
            return stack
        }
        return visible.toImmutableList()
    }
}
