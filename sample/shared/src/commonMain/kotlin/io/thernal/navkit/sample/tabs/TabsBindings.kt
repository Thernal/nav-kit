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

private class TabsGraph(private val session: SessionStore) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<SingleHostTabsRoute> { SingleHostTabsScreen() }
        navEntry<PerTabStacksRoute> { PerTabStacksScreen(session) }
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
                group = "Nested navigation",
                kind = ExampleKind.SIMPLE,
                title = "One host, tabs as its stack",
                summary = "A bottom bar over a single nested host; selecting a tab replaces the stack.",
                route = SingleHostTabsRoute,
            )
        }

        @Provides
        @IntoSet
        fun providePerTabStacksExample(): SampleExample {
            return SampleExample(
                group = "Nested navigation",
                kind = ExampleKind.REAL_LIFE,
                title = "A stack per tab",
                summary = "Depth survives a tab switch, and one tab is a guarded graph.",
                route = PerTabStacksRoute,
            )
        }
    }
}
