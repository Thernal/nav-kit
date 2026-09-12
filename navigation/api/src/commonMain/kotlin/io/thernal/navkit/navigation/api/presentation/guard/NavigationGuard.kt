package io.thernal.navkit.navigation.api.presentation.guard

import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Evaluated by [io.thernal.navkit.navigation.api.presentation.navigator.Navigator] before a route
 * reaches the back stack — never during composition, so a blocked route never renders.
 *
 * A guard applies to a subset of routes, not all of them. Mark the routes it applies to with a
 * dedicated marker interface named after the guard with a `-Guarded` suffix (`AuthGuard` guards
 * routes marked `AuthGuarded`), and check that marker first:
 *
 * ```
 * interface AuthGuarded : Route
 *
 * class AuthGuardImpl(private val session: Session) : NavigationGuard {
 *     override fun evaluate(route: Route): GuardResult {
 *         if (route !is AuthGuarded) return GuardResult.Allow
 *         return if (session.isAuthenticated) GuardResult.Allow else GuardResult.Redirect(SignIn)
 *     }
 * }
 * ```
 *
 * This keeps "which routes need this guard" declared on the route itself instead of in a separate
 * registry that has to be kept in sync, and every injected guard can run against every route
 * unconditionally — [NavigationGuardRunner] does not filter by route type itself.
 */
fun interface NavigationGuard {
    fun evaluate(route: Route): GuardResult
}
