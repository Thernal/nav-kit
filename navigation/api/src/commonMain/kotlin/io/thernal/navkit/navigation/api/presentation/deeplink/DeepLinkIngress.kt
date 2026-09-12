package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkSource

/**
 * Publishes a runtime deep link — cold start, a new intent, a notification tap, or a
 * `UIApplicationDelegate` URL callback.
 *
 * Platform-shaped overloads stay out of the common contract: Android's `Intent` form is an
 * extension in `androidMain`, so an iOS caller never sees a member it cannot satisfy.
 */
interface DeepLinkIngress {
    fun publish(
        uri: String,
        source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
    ): Boolean
}
