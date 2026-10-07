package io.thernal.navkit.sample.shared.sheets

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface SheetsProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideSheetsGraphProvider(): NavigationGraphProvider {
            return SheetsGraphProvider()
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
