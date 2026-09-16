package io.thernal.navkit.sample.tabs

import androidx.lifecycle.ViewModel
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A back stack per tab, kept while another tab is showing.
 *
 * The stacks live here rather than in the host, which is exactly what makes this possible: the host
 * is controlled, so switching tabs is a matter of handing it a different list. Nothing about the
 * kit had to change to support it.
 */
class PerTabStacksViewModel : ViewModel() {
    private val stacks: MutableStateFlow<Map<Tab, ImmutableList<Route>>> = MutableStateFlow(
        mapOf(
            Tab.FEED to persistentListOf(FeedList),
            Tab.SAVED to persistentListOf(SavedList),
            Tab.ADMIN to persistentListOf(AdminDashboard),
        ),
    )
    private val selected = MutableStateFlow(Tab.FEED)

    val current: StateFlow<Tab> = selected.asStateFlow()

    val backStack: StateFlow<Map<Tab, ImmutableList<Route>>> = stacks.asStateFlow()

    fun select(tab: Tab) {
        // Re-selecting the tab you are already on resets it to its root, which is what a tab bar
        // is expected to do and costs one line when the stacks are yours.
        if (selected.value == tab) {
            stacks.value = stacks.value + (tab to persistentListOf(rootOf(tab)))
            return
        }
        selected.value = tab
    }

    fun onBackStackChange(next: ImmutableList<Route>) {
        stacks.value = stacks.value + (selected.value to next)
    }

    private fun rootOf(tab: Tab): Route {
        return when (tab) {
            Tab.FEED -> FeedList
            Tab.SAVED -> SavedList
            Tab.ADMIN -> AdminDashboard
        }
    }
}
