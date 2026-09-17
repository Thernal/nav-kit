package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList

/**
 * One deferral a host is waiting on, with the stack whose command produced it.
 *
 * A plain class rather than a data class on purpose: the host's effect is keyed on its identity, so
 * two submissions are two runs however alike they look.
 */
internal class PendingDeferral(
    val attempted: ImmutableList<Route>,
    val deferral: GuardVerdict.Deferred,
)

/**
 * The deferral a host is awaiting — at most one at a time.
 *
 * Two things feed it: the host's navigator, when a command it ran was deferred, and the host
 * itself, when a stack it was handed or revalidated was. One thing drains it: the host's effect,
 * keyed on [pending]. Keying the run here rather than on the verdict is the point. The verdict is
 * derived from the stack, a deferral's own placeholder push changes the stack, and a run keyed on
 * the verdict cancelled itself the moment it showed its prompt.
 *
 * A run ends in one of three ways: it settles and its stack is applied, a newer deferral replaces
 * it, or it is [abandon]ed because the stack moved without it — the user backed out of the prompt,
 * a deep link arrived, the host was handed another list.
 */
@Stable
internal class HostDeferrals {
    var pending: PendingDeferral? by mutableStateOf(null)
        private set

    /**
     * Starts waiting on [deferral], unless a run for the same [attempted] stack already is: a double
     * tap, or a revalidation landing before the host's write-back, must not show the prompt twice.
     */
    fun submit(
        attempted: ImmutableList<Route>,
        deferral: GuardVerdict.Deferred,
    ) {
        if (pending?.attempted == attempted) {
            return
        }
        pending = PendingDeferral(attempted = attempted, deferral = deferral)
    }

    fun abandon() {
        pending = null
    }

    /**
     * Awaits [run] through [navigator] and applies what it settled on.
     *
     * [navigator] must be one that neither abandons nor submits: the run's own placeholder push is
     * not the user leaving, and a deferral that defers again is left alone rather than spun on.
     *
     * A run abandoned before it started, or while it was finishing, does nothing. Cancelling the
     * effect waits for the next composition: the host can launch a run in the same frame that
     * abandoned it, and a run's answer can arrive before the cancellation does.
     */
    suspend fun drive(
        run: PendingDeferral,
        navigator: Navigator,
    ) {
        if (pending !== run) {
            return
        }
        val settled = run.deferral.resolve(navigator)
        if (pending !== run) {
            return
        }
        pending = null
        if (settled is GuardVerdict.Resolved) {
            navigator.buildStack {
                clear()
                addAll(settled.stack)
            }
        }
    }
}
