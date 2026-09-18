package io.thernal.navkit.navigation.api.presentation.back

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The dispatcher the mounted `NavigationHost` consults before popping. Provided by the host, so a
 * composable registers against the same object back is dispatched through — one mechanism, not two.
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
