package io.thernal.navkit.sample.shared.catalog

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.SampleExample
import kotlinx.collections.immutable.ImmutableList

internal class CatalogGraphProvider(private val examples: ImmutableList<SampleExample>) :
    NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<CatalogRoute> { CatalogScreen(examples) }
    }
}
