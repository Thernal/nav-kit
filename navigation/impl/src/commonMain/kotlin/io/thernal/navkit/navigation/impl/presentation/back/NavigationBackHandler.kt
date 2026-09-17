package io.thernal.navkit.navigation.impl.presentation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher

/**
 * Intercepts back ahead of the host's own pop, for as long as this composable is in the composition.
 * It registers against the dispatcher the mounted host provides — the same object back is dispatched
 * through, whether it came from the gesture or from a screen calling `popBack()`.
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
