package io.thernal.navkit.sample.shared.deeplinks

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.sample.shared.catalog.CatalogRoute

/**
 * A link that lands several screens deep, which is where handlers earn their keep: a link is not a
 * destination, it is a *stack*, and the one that opens an order should leave the order list behind
 * it so back goes somewhere sensible.
 *
 * It also reads the source, because not every link deserves the same trust — a push notification
 * may open more than an external link is allowed to.
 */
class OrdersDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("orders")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        if (request.source == DeepLinkSource.IN_APP_NOTIFICATION) {
            return DeepLinkOutcome.Rejected(reason = "In-app notifications do not deep link here")
        }
        val id = request.deepLink.pathSegments.getOrNull(1)
        val base = listOf(CatalogRoute, LinkCampaignRoute, OrdersRoute)
        if (id == null) {
            return DeepLinkOutcome.Navigate(routes = base)
        }
        return DeepLinkOutcome.Navigate(routes = base + OrderRoute(id = id))
    }
}
