package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Decides which back stacks are allowed to exist. Evaluated before a stack reaches the host, never
 * during composition, so a refused route never renders.
 *
 * A guard sees the whole transition rather than a single destination: [old] is the stack as it
 * stands, [new] the one being proposed. Every decision is then expressible as the stack it returns:
 *
 * - allow — `GuardVerdict.Resolved(new)`
 * - block — `GuardVerdict.Resolved(old, reason)`, which also covers blocking a *pop*, something a
 *   destination-only guard cannot express at all
 * - redirect, with the original intent preserved — `Resolved((old + SignIn(next = new.last()))
 *   .toImmutableList())`
 *
 * Guards are folded in contribution order, each seeing the previous one's output as [new], and the
 * fold repeats until the stack stops changing — so a route a guard *introduced* is guarded like any
 * other, including by the guard that introduced it. [evaluate] must therefore be **pure and
 * cheap**: it runs once per guard per round on every navigation, and again whenever the host
 * revalidates a stack it was handed directly.
 *
 * A guard may drop routes and insert routes, but it may not reorder the ones it keeps and it may
 * not empty a non-empty stack. [NavigationGuardRunner] rejects a verdict that does either, loudly
 * — a guard returning a malformed stack corrupts navigation state for every other feature.
 *
 * Most guards apply to a subset of routes and should extend [RouteGuard] instead of implementing
 * this directly: it narrows the stack to the routes its marker names, in one place, under a `final
 * override` a subclass cannot skip. Implement this interface directly when the rule is genuinely
 * about the transition — refusing to leave a screen with unsaved work — since that is the only
 * kind of rule that needs `old` at all.
 *
 * **Keep a transition rule narrow.** `old` does not move during the fold: it stays the stack that
 * was in effect when resolution started, not the previous guard's output. So a rule broad enough to
 * reject *any* difference between the two will undo every rewriting guard, which will rewrite
 * again, and the pair fails at the round limit instead of settling. Phrase such a rule about one
 * screen's own routes — "`Edit` may not leave the stack" — never about movement in general.
 */
fun interface NavigationGuard {
    /**
     * Emits when this guard's answer may have changed — a session ending, a role arriving, a flag
     * flipping. A mounted host revalidates its stack in place on every emission, which is how a
     * route that has *become* invalid leaves the stack instead of sitting there until something
     * else happens to navigate.
     *
     * Make it hot (a `StateFlow`, a `SharedFlow`, or something `shareIn`-ed): every mounted host
     * collects it, so a cold flow that does work per collector does that work once per host.
     *
     * The default emits nothing, which is right for a guard whose answer depends only on the route.
     */
    val invalidations: Flow<Unit>
        get() = emptyFlow()

    fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict
}
