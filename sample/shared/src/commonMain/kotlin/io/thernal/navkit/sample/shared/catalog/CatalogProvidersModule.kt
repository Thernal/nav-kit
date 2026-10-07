package io.thernal.navkit.sample.shared.catalog

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.SampleExample
import kotlinx.collections.immutable.toImmutableList

@BindingContainer
@ContributesTo(AppScope::class)
interface CatalogProvidersModule {
    companion object {
        @Provides
        @IntoSet
        fun provideCatalogGraphProvider(examples: Set<SampleExample>): NavigationGraphProvider {
            val ordered = examples
                .sortedWith(compareBy({ it.topic.ordinal }, { it.kind.ordinal }, { it.title }))
                .toImmutableList()
            return CatalogGraphProvider(ordered)
        }
    }
}
