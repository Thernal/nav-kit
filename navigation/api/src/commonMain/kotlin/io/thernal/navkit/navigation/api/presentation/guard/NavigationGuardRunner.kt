package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow

/**
 * Folds every contributed [NavigationGuard] into one decision about a whole back stack.
 *
 * [resolve] is called from exactly two places, which between them are every way a stack can change:
 * the navigator's single mutating primitive, and the host, before it renders a stack it was handed
 * directly — a deep link the composition root applied through `onBackStackChange`, or state
 * restored after process death.
 *
 * To revalidate a stack that has not moved but whose guards may now answer differently, pass it as
 * both arguments: `resolve(old = current, new = current)`. Guards written as [RouteGuard] judge
 * every matching route in `new` rather than only the ones entering, so that asks the real question;
 * only a guard about a transition sees nothing to do, which is correct — no transition happened.
 */
interface NavigationGuardRunner {
    /**
     * Every contributed guard's [NavigationGuard.invalidations], merged. A host collects this for
     * as long as it is mounted and revalidates its own stack on each emission — so an unmounted
     * host costs nothing, and revalidation is scoped to what is actually on screen.
     */
    val invalidations: Flow<Unit>

    /**
     * The synchronous answer. A guard that defers is taken at its
     * [GuardVerdict.Deferred.meanwhile] and the deferral itself is dropped, so this can be called
     * from anywhere without starting work — including from composition.
     */
    fun resolve(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict.Resolved

    /**
     * The same fold, but surfacing the first deferral instead of collapsing it. For the two callers
     * that can report or await one: the navigator, which tells its caller the command is pending,
     * and the host, which is the only thing that actually runs it.
     */
    fun resolveDeferrable(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict

    /**
     * A runner that applies this one's guards and [guards] as well, for a host that contributes its
     * own — a wizard's internal rules, or a guard whose dependencies live in a feature scope rather
     * than the application graph and so could never reach the app-wide multibinding.
     *
     * Returns `this` for an empty list, so the ordinary host allocates nothing. Contributed guards
     * run after the app-wide ones; with the fold repeating to a fixpoint that ordering only decides
     * the intermediate steps, never which rule ultimately holds.
     */
    fun extendedWith(guards: ImmutableList<NavigationGuard>): NavigationGuardRunner
}
