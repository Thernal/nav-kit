package io.thernal.navkit.sample.shared.results

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
interface ResultsProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideResultsGraphProvider(): NavigationGraphProvider {
            return ResultsGraphProvider()
        }

        @Provides
        @IntoSet
        fun providePickerExample(): SampleExample {
            return SampleExample(
                topic = Topic.RESULTS,
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
                topic = Topic.RESULTS,
                kind = ExampleKind.ADVANCED,
                title = "Review request",
                summary = "A three-step flow returns one decision to the screen that launched it.",
                route = ReviewHomeRoute,
            )
        }
    }
}
