package io.thernal.navkit.sample.arguments

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample

private class ArgumentsGraph : NavigationGraphProvider {
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

@BindingContainer
@ContributesTo(AppScope::class)
interface ArgumentsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideArgumentsGraph(): NavigationGraphProvider {
            return ArgumentsGraph()
        }

        @Provides
        @IntoSet
        fun provideGreetingExample(): SampleExample {
            return SampleExample(
                group = "Arguments",
                kind = ExampleKind.SIMPLE,
                title = "One value, two screens",
                summary = "Set it before the push; read it without threading it through a route.",
                route = GreetingSetupRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideCheckoutExample(): SampleExample {
            return SampleExample(
                group = "Arguments",
                kind = ExampleKind.REAL_LIFE,
                title = "Checkout draft",
                summary = "One draft read and updated on four screens, pruned when the flow ends.",
                route = CheckoutStartRoute,
            )
        }
    }
}
