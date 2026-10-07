package io.thernal.navkit.sample.sheets

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.host.bottomSheetEntry
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample
import io.thernal.navkit.sample.ui.Topic

/** The whole difference between a sheet and a full screen: which builder registers the route. */
private class SheetsGraph : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<PostRoute> { PostScreen() }
        bottomSheetEntry<ShareSheetRoute> { ShareSheet() }

        navEntry<BasketRoute> { BasketScreen() }
        bottomSheetEntry<PaymentMethodSheetRoute> { PaymentMethodSheet() }
        bottomSheetEntry<AddCardSheetRoute> { AddCardSheet() }
        bottomSheetEntry<ConfirmCardSheetRoute> { route -> ConfirmCardSheet(route) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface SheetsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideSheetsGraph(): NavigationGraphProvider {
            return SheetsGraph()
        }

        @Provides
        @IntoSet
        fun provideShareSheetExample(): SampleExample {
            return SampleExample(
                topic = Topic.BOTTOM_SHEETS,
                kind = ExampleKind.SIMPLE,
                title = "Share sheet",
                summary = "One entry, one metadata key: the same push lands on the sheet surface.",
                route = PostRoute,
            )
        }

        @Provides
        @IntoSet
        fun providePaymentSheetExample(): SampleExample {
            return SampleExample(
                topic = Topic.BOTTOM_SHEETS,
                kind = ExampleKind.ADVANCED,
                title = "Payment method",
                summary = "A sheet pushes a sheet: one panel, a back stack of its own, one result.",
                route = BasketRoute,
            )
        }
    }
}
