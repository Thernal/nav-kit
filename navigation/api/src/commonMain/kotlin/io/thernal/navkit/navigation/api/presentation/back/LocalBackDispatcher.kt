package io.thernal.navkit.navigation.api.presentation.back

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The dispatcher the mounted `NavigationHost` consults before popping. `impl` provides the injected
 * instance here so a composable registers against the same object the host dispatches through —
 * one back mechanism, not two.
 */
val LocalBackDispatcher = staticCompositionLocalOf<BackDispatcher> { NoOpBackDispatcher }

/** Consumes nothing, so a composable outside any `NavigationHost` still composes. */
private object NoOpBackDispatcher : BackDispatcher {
    override fun register(callback: BackCallback): AutoCloseable {
        return AutoCloseable { }
    }

    override fun dispatch(): Boolean {
        return false
    }

    override fun hasCallbacks(): Boolean {
        return false
    }
}
