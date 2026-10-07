package io.thernal.navkit.sample.shared.guards

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * The marker a guard narrows to, named after the guard that reads it.
 *
 * `RouteGuard<AuthGuarded>` takes the narrowing in its constructor, so the pairing is checked by
 * the compiler rather than repeated as a convention in every implementation — and a subclass cannot
 * skip it and silently apply itself to every route in the application.
 */
interface AuthGuarded : Route
