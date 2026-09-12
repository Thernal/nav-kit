package io.thernal.navkit.navigation.api.presentation.deeplink

import android.content.Intent
import io.thernal.navkit.navigation.api.domain.DeepLinkSource

/** Cold start and `onNewIntent` hand an `Intent`; everything else is the common [publish]. */
fun DeepLinkIngress.publish(
    intent: Intent,
    source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
): Boolean {
    val uri = intent.dataString ?: return false
    return publish(uri = uri, source = source)
}
