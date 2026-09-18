package io.thernal.navkit.navigation.impl.presentation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher

/**
 * Intercepts back ahead of the host's own pop, while this composable is composed. It registers
 * against the dispatcher the host provides — the same object back is dispatched through, however it
 * was triggered.
 */
@Composable
fun NavigationBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    val dispatcher = LocalBackDispatcher.current
    val currentOnBack by rememberUpdatedState(onBack)
    val isCurrentlyEnabled by rememberUpdatedState(enabled)

    // Keyed on the dispatcher alone: `enabled` and `onBack` are read through the state above, so
    // toggling either cannot reorder this callback among its siblings.
    DisposableEffect(dispatcher) {
        val registration = dispatcher.register(
            BackCallback {
                if (isCurrentlyEnabled) {
                    currentOnBack()
                    true
                } else {
                    false
                }
            },
        )
        onDispose { registration.close() }
    }
}
