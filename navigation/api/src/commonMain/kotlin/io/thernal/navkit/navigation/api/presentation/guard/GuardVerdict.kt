package io.thernal.navkit.navigation.api.presentation.guard

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList

/**
 * What a [NavigationGuard] — or the whole [NavigationGuardRunner] — decided the back stack should
 * be.
 */
@Immutable
sealed interface GuardVerdict {
    /**
     * The stack that is allowed to exist. Returning the proposed stack unchanged is "allow";
     * returning the previous one is "block"; returning anything else is a rewrite — a redirect, an
     * injected step, a dropped entry.
     *
     * [reason] is carried only so a refusal can be reported. It says nothing about *whether* the
     * stack was refused, which is derived by comparing the returned stack with the proposed one.
     */
    data class Resolved(
        val stack: ImmutableList<Route>,
        val reason: BlockReason? = null,
    ) : GuardVerdict

    /**
     * The guard cannot answer yet — a token to refresh, a confirmation to collect, a server to ask.
     *
     * [meanwhile] is what exists until it can: `old` holds the navigation without showing anything
     * new, `old + Loading` shows a placeholder. [resolve] is then awaited off the navigator, and
     * whatever it returns is applied — so returning `Resolved(new)` from it continues to the route
     * the user originally asked for, which is the whole point of deferring rather than redirecting.
     *
     * **A guard that defers must answer synchronously once its deferral has settled**, from a
     * cached result. It is asked again as soon as the settled stack is applied, and a guard that
     * defers a second time for the same stack simply never converges.
     *
     * Only the mounted host awaits a deferral: it owns a scope tied to composition, so an unmounted
     * host cancels what it started, and there is exactly one driver no matter how many ways the
     * stack can change. A navigator command that is deferred hands its deferral to that host. The
     * host waits on one deferral at a time, keyed on the stack that was attempted, so the same one is
     * never launched twice — and not on the verdict, which the placeholder push itself changes.
     *
     * A deferral is abandoned when the stack moves without it: a command that changes the stack
     * (backing out of the placeholder, say), or a stack the host did not write (a deep link, a
     * different list handed in). The [Navigator] passed to [resolve] does neither, so its own pushes
     * are part of the wait, and a deferral it meets is left alone rather than started.
     */
    data class Deferred(
        val meanwhile: ImmutableList<Route>,
        val resolve: suspend (Navigator) -> GuardVerdict,
    ) : GuardVerdict
}
