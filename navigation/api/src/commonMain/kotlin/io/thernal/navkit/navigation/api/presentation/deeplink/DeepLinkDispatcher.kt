package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkSource

/** Routes a raw link to the handler that owns its page. */
interface DeepLinkDispatcher {
    suspend fun dispatch(
        raw: String,
        source: DeepLinkSource,
    ): DeepLinkOutcome
}
