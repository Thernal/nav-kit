package io.thernal.navkit.sample.shared.sheets

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.bottomSheetEntry
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

/** The whole difference between a sheet and a full screen: which builder registers the route. */
internal class SheetsGraphProvider : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<PostRoute> { PostScreen() }
        bottomSheetEntry<ShareSheetRoute> { ShareSheet() }

        navEntry<BasketRoute> { BasketScreen() }
        bottomSheetEntry<PaymentMethodSheetRoute> { PaymentMethodSheet() }
        bottomSheetEntry<AddCardSheetRoute> { AddCardSheet() }
        bottomSheetEntry<ConfirmCardSheetRoute> { route -> ConfirmCardSheet(route) }
    }
}
