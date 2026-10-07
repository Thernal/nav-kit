package io.thernal.navkit.sample.tabs

import androidx.lifecycle.ViewModel
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * One screen, one nested host, and the tabs are that host's stack.
 *
 * This is the simple shape, and it is simple because there is only one back stack to think about:
 * selecting a tab replaces it, going deeper pushes onto it, and back pops it. State that a tab's
 * screen holds per entry is cleared when the tab is left, because leaving is a pop.
 */
class SingleHostTabsViewModel : ViewModel() {
    private val state: MutableStateFlow<ImmutableList<Route>> =
        MutableStateFlow(persistentListOf(HomeTab))

    val backStack: StateFlow<ImmutableList<Route>> = state.asStateFlow()

    val current: Route
        get() = state.value.last()

    fun select(tab: SingleHostTab) {
        // Replacing rather than pushing is what makes the bar a tab bar: there is never a stack of
        // tabs, only a stack *inside* the selected one.
        state.value = persistentListOf(tab)
    }

    fun onBackStackChange(next: ImmutableList<Route>) {
        state.value = next
    }
}
