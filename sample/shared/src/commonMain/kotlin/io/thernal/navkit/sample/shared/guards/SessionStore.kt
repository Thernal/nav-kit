package io.thernal.navkit.sample.guards

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/** Whether there is a session. Application-scoped, because a guard is not part of any screen. */
class SessionStore {
    private val state = MutableStateFlow(false)

    val signedIn: StateFlow<Boolean> = state.asStateFlow()

    /**
     * What a guard announces as its [io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard.invalidations].
     *
     * `drop(1)` because a `StateFlow` replays its current value to every new collector, and a
     * mounted host collecting this on mount would revalidate once for nothing. What the host needs
     * to hear is that the answer *changed*.
     */
    val changes: Flow<Unit> = state.drop(1).map { }

    fun signIn() {
        state.value = true
    }

    fun signOut() {
        state.value = false
    }
}
