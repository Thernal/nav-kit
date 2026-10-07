package io.thernal.navkit.sample.guards

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.TransientRoute
import io.thernal.navkit.sample.app.SampleRoute

sealed interface GuardsRoute : SampleRoute

/**
 * The marker a guard narrows to, named after the guard that reads it.
 *
 * `RouteGuard<AuthGuarded>` takes the narrowing in its constructor, so the pairing is checked by
 * the compiler rather than repeated as a convention in every implementation — and a subclass cannot
 * skip it and silently apply itself to every route in the application.
 */
interface AuthGuarded : Route

/** The same idea for the second guard: a route that may only be seen behind a PIN. */
interface PinProtected : Route

data object MembersHomeRoute : GuardsRoute

data object MembersSecretRoute : GuardsRoute, AuthGuarded

/**
 * Carries the route the user was actually going to, so signing in continues there instead of
 * dropping them somewhere arbitrary. That is the difference between a redirect that preserves
 * intent and one that loses it.
 */
data class SignInRoute(val next: Route?) : GuardsRoute

data object VaultLobbyRoute : GuardsRoute

data object VaultRoute : GuardsRoute, PinProtected

/**
 * A placeholder the *guard* puts on the stack while it waits for the PIN.
 *
 * `TransientRoute` is the part that matters: routes survive process death and an in-flight coroutine
 * does not, so without this marker a restored stack could come back showing a PIN screen with
 * nothing left to resolve it — a screen nobody can leave. The host drops these from a restored
 * stack before anything is rendered or guarded.
 */
data object PinEntryRoute : GuardsRoute, TransientRoute

data object SignInRequired : BlockReason {
    override val message: String = "Sign in to continue"
}

data object PinRequired : BlockReason {
    override val message: String = "The vault stays locked"
}
