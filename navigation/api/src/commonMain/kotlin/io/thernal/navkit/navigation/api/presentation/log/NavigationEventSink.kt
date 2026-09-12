package io.thernal.navkit.navigation.api.presentation.log

/**
 * Where [NavigationEvent]s go. An injected seam rather than a global logger object: a test
 * substitutes a recording sink and asserts on what the navigator emitted, which is the whole
 * reason the type exists.
 */
fun interface NavigationEventSink {
    fun emit(event: NavigationEvent)

    companion object {
        /** The binding an app gets until it contributes its own. */
        val NoOp: NavigationEventSink = NavigationEventSink { }
    }
}
