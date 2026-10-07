package io.thernal.navkit.sample.shared.results

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

internal class ResultsGraphProvider : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<PickerHomeRoute> { PickerHomeScreen() }
        navEntry<PickerRoute> { PickerScreen() }
        navEntry<ReviewHomeRoute> { ReviewHomeScreen() }
        navEntry<ReviewStepRoute> { route -> ReviewStepScreen(route) }
    }
}
