package io.thernal.navkit.sample.basics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val NOTHING_YET = "—"

/**
 * The last command the order flow ran and what came of it, kept outside every step.
 *
 * A command's outcome is known to the screen that issued it, and that screen is usually the one the
 * command takes away — pushed over, popped, replaced. Held in the step, the readout was thrown away
 * with the step and always showed "—".
 */
class OrderFlowLog {
    private val mutableLast = MutableStateFlow(NOTHING_YET)

    val last: StateFlow<String> = mutableLast.asStateFlow()

    fun record(
        command: String,
        outcome: String,
    ) {
        mutableLast.value = "$command → $outcome"
    }

    fun reset() {
        mutableLast.value = NOTHING_YET
    }
}
