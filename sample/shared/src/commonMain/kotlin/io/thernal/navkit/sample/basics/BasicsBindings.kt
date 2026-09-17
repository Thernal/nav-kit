package io.thernal.navkit.sample.basics

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample

/**
 * What a feature module contributes to the application: its screens, and its place in the index.
 * The composition root imports neither.
 */
private class BasicsGraph(private val log: OrderFlowLog) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<BasicsHomeRoute> { BasicsHomeScreen() }
        navEntry<BasicsDetailRoute> { route -> BasicsDetailScreen(route) }
        navEntry<WizardRoute> { WizardScreen(log) }
        navEntry<WizardStepRoute> { route -> WizardStepScreen(route = route, log = log) }
        navEntry<WizardDoneRoute> { WizardDoneScreen(log) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface BasicsBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideOrderFlowLog(): OrderFlowLog {
            return OrderFlowLog()
        }

        @Provides
        @IntoSet
        fun provideBasicsGraph(log: OrderFlowLog): NavigationGraphProvider {
            return BasicsGraph(log)
        }

        @Provides
        @IntoSet
        fun provideBasicsSimpleExample(): SampleExample {
            return SampleExample(
                group = "Navigation",
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
                group = "Navigation",
                kind = ExampleKind.REAL_LIFE,
                title = "Order flow",
                summary = "navigate with a predicate, replaceAll, popBackTo, and reading outcomes.",
                route = WizardRoute,
            )
        }
    }
}
