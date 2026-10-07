package io.thernal.navkit.sample.shared.deeplinks

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.sample.shared.catalog.CatalogRoute
import io.thernal.navkit.sample.shared.guards.MembersSecretRoute

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
