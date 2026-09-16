package io.thernal.navkit.sample.guards

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.guard.RouteGuard
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * The shape almost every guard wants: a rule about a set of destinations.
 *
 * It judges **every** matching route in the proposed stack, not only the ones entering on this
 * transition, and answers by substitution. Both follow from the same requirement — "the secret area
 * requires a session" has to hold for any stack that contains it, however it got there — which is
 * also exactly what makes it correct when the host revalidates a stack in place and there is no
 * transition to reason about.
 *
 * [invalidations] is what makes signing *out* work. Nothing navigates when a session ends, so
 * without it the guarded screen would sit there until something else happened to move the stack.
 * Every mounted host collects this and revalidates its own stack on each emission.
 */
class AuthGuard(private val session: SessionStore) : RouteGuard<AuthGuarded>({ it as? AuthGuarded }) {
    override val reason: BlockReason = SignInRequired

    override val invalidations: Flow<Unit> = session.changes

    override fun redirect(
        route: AuthGuarded,
        stack: ImmutableList<Route>,
    ): Route? {
        if (session.signedIn.value) {
            return null
        }
        return SignInRoute(next = route)
    }
}
