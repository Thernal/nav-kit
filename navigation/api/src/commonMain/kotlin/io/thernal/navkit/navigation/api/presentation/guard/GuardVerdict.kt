package io.thernal.navkit.navigation.api.presentation.guard

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList

/** What a [NavigationGuard] — or the whole [NavigationGuardRunner] — decided the stack should be. */
@Immutable
sealed interface GuardVerdict {
    /**
     * The stack that is allowed to exist. [reason] is carried only so a refusal can be reported;
     * whether it was a refusal is derived by comparing this stack with the proposal.
     */
    data class Resolved(
        val stack: ImmutableList<Route>,
        val reason: BlockReason? = null,
    ) : GuardVerdict

    /**
     * The guard cannot answer yet: [meanwhile] exists until it can, and whatever [resolve] returns is
     * applied. **It must answer synchronously the second time**, from a cached result — deferring
     * again for the same stack never converges. See `navigation/README.md`, "Deciding later".
     */
    data class Deferred(
        val meanwhile: ImmutableList<Route>,
        val resolve: suspend (Navigator) -> GuardVerdict,
    ) : GuardVerdict
}
