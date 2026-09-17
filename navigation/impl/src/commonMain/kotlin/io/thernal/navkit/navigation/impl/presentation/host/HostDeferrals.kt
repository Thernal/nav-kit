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
 * One deferral a host is waiting on, with the stack whose command produced it. A plain class, not a
 * data one: the host's effect is keyed on its identity, so two submissions are two runs.
 */
internal class PendingDeferral(
    val attempted: ImmutableList<Route>,
    val deferral: GuardVerdict.Deferred,
)

/**
 * The deferral a host is awaiting — at most one at a time. **Keyed on the run, never on the
 * verdict**: a deferral's own placeholder push changes the stack, so a run keyed on the verdict
 * cancels itself the moment it shows its prompt.
 */
@Stable
internal class HostDeferrals {
    var pending: PendingDeferral? by mutableStateOf(null)
        private set

    /**
     * Starts waiting on [deferral], unless a run for the same [attempted] stack already is: a double
     * tap, or a revalidation landing before the write-back, must not show the prompt twice.
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
     * Awaits [run] through [navigator], which must neither abandon nor submit. Both identity checks
     * are load-bearing: cancellation waits for the next composition, so an answer can beat it.
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
