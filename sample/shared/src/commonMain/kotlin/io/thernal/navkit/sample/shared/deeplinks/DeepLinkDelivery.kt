package io.thernal.navkit.sample.shared.deeplinks

import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.sample.shared.app.DeepLinkLog

/**
 * Hands [uri] to the ingress. What a handler makes of it arrives later, through [log]; only a link
 * the ingress itself turns away is recorded here.
 */
internal fun deliver(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
    uri: String,
    source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
) {
    if (!ingress.publish(uri = uri, source = source)) {
        log.recordRefused(uri)
    }
}
