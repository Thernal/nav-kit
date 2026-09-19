package io.thernal.navkit.sample.tabs

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
import io.thernal.navkit.sample.guards.SessionStore
import io.thernal.navkit.sample.ui.Topic

private class TabsGraph(private val session: SessionStore) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<SingleHostTabsRoute> { SingleHostTabsScreen(session) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface TabsBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideTabsGraph(session: SessionStore): NavigationGraphProvider {
            return TabsGraph(session)
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
