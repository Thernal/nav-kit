package io.thernal.navkit.sample.results

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

private class ResultsGraph : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<PickerHomeRoute> { PickerHomeScreen() }
        navEntry<PickerRoute> { PickerScreen() }
        navEntry<ReviewHomeRoute> { ReviewHomeScreen() }
        navEntry<ReviewStepRoute> { route -> ReviewStepScreen(route) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface ResultsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideResultsGraph(): NavigationGraphProvider {
            return ResultsGraph()
        }

        @Provides
        @IntoSet
        fun providePickerExample(): SampleExample {
            return SampleExample(
                group = "Results",
                kind = ExampleKind.SIMPLE,
                title = "Pick a colour",
                summary = "A picker posts a typed result; the screen behind it consumes once.",
                route = PickerHomeRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideReviewExample(): SampleExample {
            return SampleExample(
                group = "Results",
                kind = ExampleKind.REAL_LIFE,
                title = "Review request",
                summary = "A three-step flow returns one decision to the screen that launched it.",
                route = ReviewHomeRoute,
            )
        }
    }
}
