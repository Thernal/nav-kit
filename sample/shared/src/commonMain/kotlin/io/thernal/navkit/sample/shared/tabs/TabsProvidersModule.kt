package io.thernal.navkit.sample.shared.tabs

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.guards.SessionStore
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface TabsProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideTabsGraphProvider(session: SessionStore): NavigationGraphProvider {
            return TabsGraphProvider(session)
        }

        @Provides
        @IntoSet
        fun provideSingleHostTabsExample(): SampleExample {
            return SampleExample(
                topic = Topic.NESTED_NAVIGATION,
                kind = ExampleKind.SIMPLE,
                title = "One host, tabs as its stack",
                summary = "A bottom bar over a single nested host; one tab is a guarded graph.",
                route = SingleHostTabsRoute,
            )
        }
    }
}
