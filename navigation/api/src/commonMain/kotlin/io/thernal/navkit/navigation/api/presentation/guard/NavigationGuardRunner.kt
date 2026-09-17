package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Folds every contributed [NavigationGuard] into one decision about a whole back stack.
 *
 * To revalidate a stack that has not moved but whose guards may now answer differently, pass it as
 * both arguments: `resolve(old = current, new = current)`.
 */
interface NavigationGuardRunner {
    /** Every guard's [NavigationGuard.invalidations], merged. A host collects it while mounted. */
    val invalidations: Flow<Unit>

    /**
     * The synchronous answer: a guard that defers is taken at its [GuardVerdict.Deferred.meanwhile]
     * and the deferral dropped, so this starts no work and is safe to call from composition.
     */
    fun resolve(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict.Resolved

    /** The same fold, surfacing the first deferral instead of collapsing it. */
    fun resolveDeferrable(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict

    /**
     * A runner that applies this one's guards and [guards] as well, for a host that contributes its
     * own. Returns `this` for an empty list.
     */
    fun extendedWith(guards: ImmutableList<NavigationGuard>): NavigationGuardRunner
}
