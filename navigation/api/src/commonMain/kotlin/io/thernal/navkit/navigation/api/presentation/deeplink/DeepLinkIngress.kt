package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkSource

/**
 * Publishes a runtime deep link — cold start, a new intent, a notification tap. Android's `Intent`
 * form is an `androidMain` extension, so an iOS caller never sees a member it cannot satisfy.
 */
interface DeepLinkIngress {
    fun publish(
        uri: String,
        source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
    ): Boolean
}
