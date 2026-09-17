package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Decides which back stacks may exist, before one reaches the host. Every decision is the stack it
 * returns: `Resolved(new)` allows, `Resolved(old, reason)` refuses, anything else rewrites.
 *
 * [evaluate] must be **pure and cheap** — guards are folded to a fixpoint — and may not reorder what
 * it keeps or empty the stack. Most guards extend [RouteGuard]. See `navigation/README.md`.
 */
fun interface NavigationGuard {
    /**
     * Emits when this guard's answer may have changed, which is what makes a route that has *become*
     * invalid leave the stack. **Make it hot**: every mounted host collects it.
     */
    val invalidations: Flow<Unit>
        get() = emptyFlow()

    fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict
}
