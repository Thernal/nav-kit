package io.thernal.navkit.sample.deeplinks

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.sample.catalog.CatalogRoute
import io.thernal.navkit.sample.guards.MembersSecretRoute

/**
 * One page, one route.
 *
 * A handler declares the pages it owns and the dispatcher routes by that — each page has exactly
 * one owning handler, and a duplicate is a startup error rather than a silent last-one-wins.
 *
 * The parser derives the shape from the scheme, so this handler answers `navkit://product/42` and
 * `https://example.com/product/42` without knowing either form exists: on a custom scheme the host
 * *is* the first page, on http(s) the host is a domain and only the path counts.
 */
class ProductDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("product")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val id = request.deepLink.pathSegments.getOrNull(1)
            ?: return DeepLinkOutcome.Rejected(reason = "A product link needs an id")
        return DeepLinkOutcome.Navigate(
            routes = listOf(LinkPlaygroundRoute, ProductRoute(id = id)),
        )
    }
}

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

/**
 * Resolves to a route the application's sign-in guard protects.
 *
 * The handler does not check the session and does not need to. A resolved link reaches the host
 * through the root state holder rather than through `Navigator`, and the host resolves whatever
 * stack it is handed before rendering it — so the one navigation input that comes from outside the
 * application is guarded without the root having to remember to ask.
 */
class SecretDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("secret")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        return DeepLinkOutcome.Navigate(routes = listOf(CatalogRoute, MembersSecretRoute))
    }
}
