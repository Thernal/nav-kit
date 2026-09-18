package io.thernal.navkit.navigation.impl.data

import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.domain.IncomingDeepLink
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkEvents
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Ingress shared by cold starts, new intents and notification taps. Buffered rather than conflated:
 * a link published before the root is collecting still has to arrive.
 */
class RuntimeDeepLinkBridge : DeepLinkIngress, DeepLinkEvents {
    private val channel = Channel<IncomingDeepLink>(Channel.BUFFERED)

    override val links: Flow<IncomingDeepLink> = channel.receiveAsFlow()

    override fun publish(
        uri: String,
        source: DeepLinkSource,
    ): Boolean {
        if (uri.isBlank()) {
            return false
        }
        return channel.trySend(IncomingDeepLink(uri = uri, source = source)).isSuccess
    }
}
