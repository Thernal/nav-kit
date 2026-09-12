package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * A [NavigationGuard] for the routes of one type — the shape almost every guard wants.
 *
 * Mark the routes a guard applies to with an interface named after it — `AuthGuard` guards routes
 * marked `AuthGuarded` — and pass the narrowing to this constructor, so the pairing is checked by
 * the compiler instead of by a convention every implementation has to remember to repeat:
 *
 * ```
 * interface AuthGuarded : Route
 *
 * class AuthGuardImpl(private val session: Session) : RouteGuard<AuthGuarded>({ it as? AuthGuarded }) {
 *     override val reason = SignInRequired
 *
 *     override fun redirect(
 *         route: AuthGuarded,
 *         stack: ImmutableList<Route>,
 *     ): Route? {
 *         if (session.isAuthenticated) {
 *             return null
 *         }
 *         return AuthRoute.SignIn
 *     }
 * }
 * ```
 *
 * `final override` on [evaluate] is the point: a subclass cannot skip the narrowing and silently
 * apply itself to every route in the application.
 *
 * It inspects **every** matching route in the proposed stack, not only the ones entering on this
 * transition, and answers by substitution rather than by handing back the previous stack. Both
 * follow from the same requirement: "`Secret` requires a session" has to hold for any stack that
 * contains `Secret`, however it got there — which is also exactly what makes this guard correct
 * when a stack is revalidated in place and there is no transition to reason about.
 *
 * A rule about *leaving* a screen rather than being on one — unsaved work, a confirmation — is a
 * rule about the transition, and implements [NavigationGuard] directly so it can compare `old`
 * with `new`.
 */
abstract class RouteGuard<R : Route>(private val narrow: (Route) -> R?) : NavigationGuard {

    /** Reported on the [NavigationEvent.Blocked][io.thernal.navkit.navigation.api.presentation.log.NavigationEvent.Blocked] this guard causes. */
    protected open val reason: BlockReason? = null

    final override fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict {
        var didRewrite = false
        val rewritten = new.map { route ->
            val narrowed = narrow(route)
            val replacement = narrowed?.let { matched -> redirect(route = matched, stack = new) }
            if (replacement == null) {
                route
            } else {
                didRewrite = true
                replacement
            }
        }
        if (!didRewrite) {
            return GuardVerdict.Resolved(new)
        }
        // `distinct` because two rejected routes commonly redirect to the same destination, and a
        // stack that names one route twice is not a stack the host can render.
        return GuardVerdict.Resolved(stack = rewritten.distinct().toImmutableList(), reason = reason)
    }

    /** `null` lets [route] stand; a returned route replaces it wherever it sits in [stack]. */
    protected abstract fun redirect(
        route: R,
        stack: ImmutableList<Route>,
    ): Route?
}
