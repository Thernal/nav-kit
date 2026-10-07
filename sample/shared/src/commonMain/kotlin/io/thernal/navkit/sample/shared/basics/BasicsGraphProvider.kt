package io.thernal.navkit.sample.shared.basics

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

/**
 * What a feature module contributes to the application: its screens, and its place in the index.
 * The composition root imports neither.
 */
internal class BasicsGraphProvider(private val log: OrderFlowLog) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<BasicsHomeRoute> { BasicsHomeScreen() }
        navEntry<BasicsDetailRoute> { route -> BasicsDetailScreen(route) }
        navEntry<WizardRoute> { WizardScreen(log) }
        navEntry<WizardStepRoute> { route -> WizardStepScreen(route = route, log = log) }
        navEntry<WizardDoneRoute> { WizardDoneScreen(log) }
    }
}
