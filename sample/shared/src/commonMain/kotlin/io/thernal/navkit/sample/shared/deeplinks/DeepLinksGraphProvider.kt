package io.thernal.navkit.sample.shared.deeplinks

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.DeepLinkLog

internal class DeepLinksGraphProvider(
    private val ingress: DeepLinkIngress,
    private val log: DeepLinkLog,
) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<LinkPlaygroundRoute> { LinkPlaygroundScreen(ingress = ingress, log = log) }
        navEntry<ProductRoute> { route -> ProductScreen(route) }
        navEntry<LinkCampaignRoute> { LinkCampaignScreen(ingress = ingress, log = log) }
        navEntry<OrdersRoute> { OrdersScreen() }
        navEntry<OrderRoute> { route -> OrderScreen(route) }
    }
}
