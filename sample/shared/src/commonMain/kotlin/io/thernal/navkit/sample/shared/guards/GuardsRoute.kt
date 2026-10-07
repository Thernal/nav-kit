package io.thernal.navkit.sample.shared.guards

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.TransientRoute
import io.thernal.navkit.sample.shared.app.SampleRoute

sealed interface GuardsRoute : SampleRoute

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
