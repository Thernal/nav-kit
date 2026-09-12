package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.IncomingDeepLink
import kotlinx.coroutines.flow.Flow

/** Raw links published through [DeepLinkIngress] — cold start, `onNewIntent`, notification taps. */
interface DeepLinkEvents {
    val links: Flow<IncomingDeepLink>
}
