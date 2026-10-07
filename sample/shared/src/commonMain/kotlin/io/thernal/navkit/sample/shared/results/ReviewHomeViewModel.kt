package io.thernal.navkit.sample.results

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The launcher's state holder, and where a returning result lands — `ResultEffect` forwards, the
 * state holder decides.
 *
 * Scoped to the launcher's entry, so it lives exactly as long as that entry is on the stack. State
 * remembered by the composable itself was dropped every time the flow covered it, which is every
 * time the flow was opened.
 */
class ReviewHomeViewModel : ViewModel() {
    private val mutableDecision = MutableStateFlow<ReviewDecision?>(null)

    val decision: StateFlow<ReviewDecision?> = mutableDecision.asStateFlow()

    fun onDecision(made: ReviewDecision) {
        mutableDecision.value = made
    }

    fun clear() {
        mutableDecision.value = null
    }
}
