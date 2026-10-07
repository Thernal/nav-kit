package io.thernal.navkit.sample.shared.deeplinks

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.sample.shared.catalog.CatalogRoute

/**
 * One page, one route.
 *
 * A handler declares the pages it owns and the dispatcher routes by that — each page has exactly
 * one owning handler, and a duplicate is a startup error rather than a silent last-one-wins.
 *
 * The parser removes whichever registered base a link starts with, so this handler answers
 * `navkit://product/42` and `https://example.com/product/42` without knowing either form exists. The
 * bases are the application's business, registered once in `SampleBindings`.
 */
class ProductDeepLinkHandler : DeepLinkHandler {
    override val pages: Set<String> = setOf("product")

    override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
        val id = request.deepLink.pathSegments.getOrNull(1)
            ?: return DeepLinkOutcome.Rejected(reason = "A product link needs an id")
        // The catalog stays at the bottom: a link replaces the whole stack, and one that dropped the
        // app's own root left back with nowhere to go but out of the app.
        return DeepLinkOutcome.Navigate(
            routes = listOf(CatalogRoute, LinkPlaygroundRoute, ProductRoute(id = id)),
        )
    }
}
