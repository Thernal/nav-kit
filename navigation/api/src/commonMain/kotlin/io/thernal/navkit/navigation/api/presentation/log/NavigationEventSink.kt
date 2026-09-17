package io.thernal.navkit.navigation.api.presentation.log

/**
 * Where [NavigationEvent]s go. Injected rather than global, so a test can substitute a recording sink
 * and assert on what the navigator emitted.
 */
fun interface NavigationEventSink {
    fun emit(event: NavigationEvent)

    companion object {
        /** The binding an app gets until it contributes its own. */
        val NoOp: NavigationEventSink = NavigationEventSink { }
    }
}
