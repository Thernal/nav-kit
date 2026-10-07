package io.thernal.navkit.sample.shared.basics

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface BasicsProvidersModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideOrderFlowLog(): OrderFlowLog {
            return OrderFlowLog()
        }

        @Provides
        @IntoSet
        fun provideBasicsGraphProvider(log: OrderFlowLog): NavigationGraphProvider {
            return BasicsGraphProvider(log)
        }

        @Provides
        @IntoSet
        fun provideBasicsSimpleExample(): SampleExample {
            return SampleExample(
                topic = Topic.NAVIGATION,
                kind = ExampleKind.SIMPLE,
                title = "Push and pop",
                summary = "One screen opens another and the other comes back.",
                route = BasicsHomeRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideBasicsWizardExample(): SampleExample {
            return SampleExample(
                topic = Topic.NAVIGATION,
                kind = ExampleKind.ADVANCED,
                title = "Order flow",
                summary = "navigate with a predicate, replaceAll, popBackTo, and reading outcomes.",
                route = WizardRoute,
            )
        }
    }
}
