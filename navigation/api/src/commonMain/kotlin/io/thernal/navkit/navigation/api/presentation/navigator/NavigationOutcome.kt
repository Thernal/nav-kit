package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * What actually happened to a command that adds routes.
 *
 * A navigator command is not a request that always succeeds: guards can refuse it, rewrite it into
 * something else, or need time to decide. Returning that instead of `Unit` is what lets a caller
 * tell "we moved" from "we were sent somewhere else" from "nothing happened" — none of which the
 * call site could see before, since the only report was an application-wide event stream.
 *
 * The pops return a plain `Boolean` instead: "did the stack move" is the whole question there.
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
