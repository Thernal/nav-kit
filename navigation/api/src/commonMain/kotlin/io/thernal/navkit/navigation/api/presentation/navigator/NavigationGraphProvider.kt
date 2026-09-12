package io.thernal.navkit.navigation.api.presentation.navigator

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Feature-owned Navigation3 entry registration, collected by an app composition root so the root
 * never has to import a feature's screens directly.
 */
fun interface NavigationGraphProvider {
    fun EntryProviderScope<Route>.provide()
}
