package io.thernal.navkit.sample.app

import androidx.lifecycle.ViewModel
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.sample.catalog.CatalogRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owner of the application's root back stack.
 *
 * `NavigationHost` is controlled like a `TextField` — it renders a stack and reports every change,
 * and never owns one. So something outside it has to, and a ViewModel is that something: it
 * survives a configuration change, it outlives the composition a recomposition throws away, and it
 * is where a deep link is applied, which the kit deliberately leaves to the single root state
 * holder rather than to a host that can be mounted anywhere.
 *
 * [onBackStackChange] is the only writer. Every navigator command, every guard rewrite and every
 * resolved deep link arrives through it, which is what makes this class the one place to look when
 * asking what the application's navigation state is.
 *
 * The stack is typed `Route` rather than a sealed application type on purpose: the entries come
 * from feature-owned `NavigationGraphProvider`s, and a root that named a closed set of routes would
 * have to import every feature to declare it.
 */
class RootViewModel : ViewModel() {
    private val mutableBackStack: MutableStateFlow<ImmutableList<Route>> =
        MutableStateFlow(persistentListOf(CatalogRoute))

    val backStack: StateFlow<ImmutableList<Route>> = mutableBackStack.asStateFlow()

    fun onBackStackChange(next: ImmutableList<Route>) {
        mutableBackStack.value = next
    }

    /**
     * Applies a resolved deep link. This does not go through `Navigator` — a link is the one
     * navigation input that comes from outside the app — but the mounted host still guards it,
     * because it resolves whatever stack it is handed before rendering it.
     */
    fun onDeepLink(routes: List<Route>) {
        if (routes.isEmpty()) {
            return
        }
        mutableBackStack.value = routes.toImmutableList()
    }
}
