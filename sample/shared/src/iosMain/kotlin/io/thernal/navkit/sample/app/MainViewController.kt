package io.thernal.navkit.sample.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The iOS entry point. `sample/iosApp` is a plain SwiftUI shell whose only job is to show this —
 * the whole sample is the shared composition, unchanged from what the Android activity hosts.
 */
fun MainViewController(): UIViewController {
    return ComposeUIViewController { SampleApp() }
}
