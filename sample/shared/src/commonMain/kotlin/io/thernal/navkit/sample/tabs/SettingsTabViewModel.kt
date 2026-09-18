package io.thernal.navkit.sample.tabs

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The Settings tab's own state, scoped to the screen that mounts the tabs rather than to the tab's
 * entry.
 *
 * That distinction is the whole point of this example. A tab switch replaces the host's stack, so
 * from Navigation3's side the tab that was showing was **popped** — a ViewModel scoped to that entry
 * is cleared, and the tab comes back empty. Scoping it one level up, to the entry that mounts the
 * tabs, gives it the lifetime a tab actually has: it lives as long as the tab screen is on the
 * stack above, and is cleared with it.
 */
class SettingsTabViewModel : ViewModel() {
    private val state = MutableStateFlow(0)

    val edits: StateFlow<Int> = state.asStateFlow()

    fun edit() {
        state.value += 1
    }
}
