package io.thernal.navkit.navigation.api.presentation.back

/**
 * Lets a composable (a bottom sheet, an in-screen editor) intercept back ahead of the
 * `NavigationHost`'s own handling. [register] returns an [AutoCloseable]; close it when the
 * callback should stop intercepting.
 */
interface BackDispatcher {
    fun register(callback: BackCallback): AutoCloseable

    /** Runs the registered callbacks newest-first and returns true as soon as one consumes back. */
    fun dispatch(): Boolean

    fun hasCallbacks(): Boolean
}
