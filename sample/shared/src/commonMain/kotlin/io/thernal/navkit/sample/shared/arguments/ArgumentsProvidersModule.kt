package io.thernal.navkit.sample.shared.arguments

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
interface ArgumentsProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideArgumentsGraphProvider(): NavigationGraphProvider {
            return ArgumentsGraphProvider()
        }

        @Provides
        @IntoSet
        fun provideGreetingExample(): SampleExample {
            return SampleExample(
                topic = Topic.ARGUMENTS,
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
                topic = Topic.ARGUMENTS,
                kind = ExampleKind.ADVANCED,
                title = "Checkout draft",
                summary = "One draft read and updated on four screens, pruned when the flow ends.",
                route = CheckoutStartRoute,
            )
        }
    }
}
