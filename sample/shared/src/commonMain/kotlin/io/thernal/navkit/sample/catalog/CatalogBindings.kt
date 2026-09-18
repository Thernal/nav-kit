package io.thernal.navkit.sample.catalog

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.SampleExample
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private class CatalogGraph(private val examples: ImmutableList<SampleExample>) :
    NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<CatalogRoute> { CatalogScreen(examples) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface CatalogBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideCatalogGraph(examples: Set<SampleExample>): NavigationGraphProvider {
            val ordered = examples
                .sortedWith(compareBy({ it.group }, { it.kind.sortKey() }, { it.title }))
                .toImmutableList()
            return CatalogGraph(ordered)
        }
    }
}
