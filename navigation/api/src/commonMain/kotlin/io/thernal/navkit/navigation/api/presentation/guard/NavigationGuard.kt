package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Decides which back stacks may exist. Evaluated before a stack reaches the host, never during
 * composition, so a refused route never renders.
 *
 * A guard sees the whole transition — [old] as it stands, [new] as proposed — and every decision is
 * the stack it returns: `Resolved(new)` allows, `Resolved(old, reason)` refuses, anything else
 * rewrites.
 *
 * [evaluate] must be **pure and cheap**: guards are folded to a fixpoint, so it runs several times
 * per navigation. It may drop and insert routes, but not reorder the ones it keeps or empty the
 * stack — [NavigationGuardRunner] rejects a verdict that does.
 *
 * Most guards are about the routes of one type and extend [RouteGuard] instead. Implement this
 * directly only for a rule about the *transition*, and keep such a rule about one screen's own
 * routes: [old] does not move during the fold, so a rule that rejects any difference between the
 * two fights every rewriting guard instead of settling.
 *
 * See `navigation/README.md`, "Guards".
 */
fun interface NavigationGuard {
    /**
     * Emits when this guard's answer may have changed — a session ending, a role arriving. Every
     * mounted host revalidates its stack on each emission, which is how a route that has *become*
     * invalid leaves the stack instead of sitting there until something else navigates.
     *
     * **Make it hot.** Every mounted host collects it, so a cold flow does its work once per host.
     */
    val invalidations: Flow<Unit>
        get() = emptyFlow()

    fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict
}
