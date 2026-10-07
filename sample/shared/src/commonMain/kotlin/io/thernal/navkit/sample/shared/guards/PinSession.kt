package io.thernal.navkit.sample.guards

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val CORRECT_PIN = "1234"

/**
 * Stands in for the part of an application a 401 actually lands in: something outside the UI notices
 * the session is no longer trusted and says so.
 */
class PinSession {
    private val lockedState = MutableStateFlow(false)
    private val submissions = MutableSharedFlow<String>(extraBufferCapacity = 1)

    val locked: StateFlow<Boolean> = lockedState.asStateFlow()

    val changes: Flow<Unit> = lockedState.drop(1).map { }

    /** What an HTTP interceptor would call on a 401. */
    fun lock() {
        lockedState.value = true
    }

    fun submit(pin: String) {
        submissions.tryEmit(pin)
    }

    /**
     * Suspends until a PIN is submitted, and reports whether it unlocked the session.
     *
     * Once it has, [locked] is false — which is what lets the guard answer *synchronously* the next
     * time it is asked. A guard that defers a second time for the same stack never converges.
     */
    suspend fun awaitUnlock(): Boolean {
        val submitted = submissions.first()
        val didUnlock = submitted == CORRECT_PIN
        if (didUnlock) {
            lockedState.value = false
        }
        return didUnlock
    }
}
