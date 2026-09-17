package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * A [NavigationGuard] for the routes of one type — the shape almost every guard wants. Mark them
 * with an interface named after the guard (`AuthGuard` guards routes marked `AuthGuarded`) and pass
 * the narrowing, so the pairing is checked by the compiler rather than by a convention every
 * implementation has to remember:
 *
 * ```
 * class AuthGuardImpl(private val session: Session) : RouteGuard<AuthGuarded>({ it as? AuthGuarded })
 * ```
 *
 * `final override` on [evaluate] is the point: a subclass cannot skip the narrowing and silently
 * apply itself to every route in the application.
 *
 * It judges **every** matching route in the proposed stack, not only the ones entering, and answers
 * by substitution — which is what makes it correct when a stack is revalidated in place and there is
 * no transition to reason about. A rule about *leaving* a screen implements [NavigationGuard]
 * directly instead, so it can compare `old` with `new`.
 *
 * See `navigation/README.md`, "Writing one".
 */
abstract class RouteGuard<R : Route>(private val narrow: (Route) -> R?) : NavigationGuard {

    /** Carried on the navigation event this guard's refusal produces. */
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
