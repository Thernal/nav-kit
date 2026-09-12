package io.thernal.navkit.navigation.impl.domain.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkDispatcher
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome

/**
 * The page -> handler table is built once, at construction, and a page claimed twice fails there
 * rather than resolving to whichever handler the set happened to yield last.
 */
class DeepLinkDispatcherImpl(handlers: Set<DeepLinkHandler>) : DeepLinkDispatcher {
    private val handlersByPage = buildMap {
        handlers.forEach { handler ->
            handler.pages.forEach { page ->
                require(page.isNotBlank()) { "Deep link page cannot be blank" }
                val previous = put(page, handler)
                if (previous != null) {
                    error(
                        "Deep link page '$page' is claimed by ${previous.typeName()} and " +
                            handler.typeName(),
                    )
                }
            }
        }
    }

    override suspend fun dispatch(
        raw: String,
        source: DeepLinkSource,
    ): DeepLinkOutcome {
        val deepLink = parseDeepLink(raw) ?: return DeepLinkOutcome.NotFound
        val page = deepLink.page ?: return DeepLinkOutcome.NotFound
        val handler = handlersByPage[page] ?: return DeepLinkOutcome.NotFound
        return handler.resolve(DeepLinkRequest(deepLink = deepLink, source = source))
    }
}

private fun Any.typeName(): String {
    return this::class.simpleName ?: toString()
}
