package io.thernal.navkit.sample.shared.arguments

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

internal class ArgumentsGraphProvider : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<GreetingSetupRoute> { GreetingSetupScreen() }
        navEntry<GreetingReaderRoute> { GreetingReaderScreen() }
        navEntry<CheckoutStartRoute> { CheckoutStartScreen() }
        navEntry<CheckoutAmountRoute> { CheckoutAmountScreen() }
        navEntry<CheckoutAddressRoute> { CheckoutAddressScreen() }
        navEntry<CheckoutPaymentRoute> { CheckoutPaymentScreen() }
        navEntry<CheckoutSummaryRoute> { CheckoutSummaryScreen() }
    }
}
