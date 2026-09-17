package io.thernal.navkit.navigation.api.presentation.guard

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList

/** What a [NavigationGuard] — or the whole [NavigationGuardRunner] — decided the stack should be. */
@Immutable
sealed interface GuardVerdict {
    /**
     * The stack that is allowed to exist: the proposal unchanged allows, the previous stack refuses,
     * anything else rewrites. [reason] is carried only so a refusal can be reported — whether the
     * stack was refused is derived by comparing it with the proposal.
     */
    data class Resolved(
        val stack: ImmutableList<Route>,
        val reason: BlockReason? = null,
    ) : GuardVerdict

    /**
     * The guard cannot answer yet — a token to refresh, a confirmation to collect, a server to ask.
     * [meanwhile] is what exists until it can (`old` shows nothing new, `old + Loading` shows a
     * placeholder), and whatever [resolve] returns is applied — so `Resolved(new)` continues to the
     * route the user originally asked for, which a redirect cannot express.
     *
     * **A guard that defers must answer synchronously the second time**, from a cached result: it is
     * asked again as soon as the settled stack is applied, and deferring again never converges.
     *
     * Only the mounted host awaits a deferral, one at a time, and abandons it when the stack moves
     * without it — though not when the [Navigator] it hands [resolve] is what moved it. See
     * `navigation/README.md`, "Deciding later".
     */
    data class Deferred(
        val meanwhile: ImmutableList<Route>,
        val resolve: suspend (Navigator) -> GuardVerdict,
    ) : GuardVerdict
}
