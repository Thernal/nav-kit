package io.thernal.navkit.sample.app

import androidx.compose.ui.window.ComposeUIViewController
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import platform.UIKit.UIViewController

/** One graph for the life of the process, which on iOS is the life of the app. */
private val sampleGraph: SampleGraph by lazy { createSampleGraph() }

/**
 * The iOS entry point. `sample/iosApp` is a plain SwiftUI shell whose only job is to show this —
 * the whole sample is the shared composition, unchanged from what the Android activity hosts.
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController { SampleApp(sampleGraph) }
}

/**
 * What the SwiftUI shell's `onOpenURL` calls — the iOS counterpart of the Android activity's
 * `onNewIntent`, and the same single step: hand the link to the ingress and let the root resolve it.
 */
fun handleDeepLink(url: String): Boolean {
    return sampleGraph.deepLinkIngress.publish(uri = url, source = DeepLinkSource.EXTERNAL_LINK)
}
