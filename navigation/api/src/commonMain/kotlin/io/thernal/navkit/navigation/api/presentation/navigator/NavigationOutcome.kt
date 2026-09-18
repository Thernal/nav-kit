package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * What actually happened to a command that adds routes — guards can refuse it, rewrite it, or need
 * time — so a call site can tell the three apart.
 */
@Immutable
sealed interface NavigationOutcome {
    /** The stack now in effect. */
    val stack: ImmutableList<Route>

    /** The command was applied exactly as asked. */
    data class Applied(override val stack: ImmutableList<Route>) : NavigationOutcome

    /** Guards produced a different stack — a refusal, a redirect, a dropped entry. */
    data class Rewritten(
        override val stack: ImmutableList<Route>,
        val reason: BlockReason?,
    ) : NavigationOutcome

    /**
     * A guard needs time. [stack] is what exists meanwhile; the mounted host awaits the deferral
     * and applies whatever it settles on.
     */
    data class Deferred(override val stack: ImmutableList<Route>) : NavigationOutcome
}
