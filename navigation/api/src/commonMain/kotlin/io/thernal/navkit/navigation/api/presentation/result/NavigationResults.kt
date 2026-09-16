package io.thernal.navkit.navigation.api.presentation.result

import kotlinx.coroutines.flow.StateFlow

/**
 * A keyed mailbox for values travelling **backwards**: a closing screen leaves one for a screen
 * that is already in the stack and is about to be uncovered.
 *
 * It is not for arguments. A value meant for screens that do not exist yet travels forwards and
 * belongs to the route, or to the flow that owns those screens — see
 * [io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments].
 *
 * **In memory only.** A posted result is lost on process death while the routes that would have
 * consumed it are restored, so a consumer treats a missing result the way it treats a first visit,
 * never as an error.
 */
interface NavigationResults {
    /**
     * The names that currently have a value waiting. Names rather than the values themselves, so
     * one feature's pending results are not readable by every screen in the app.
     */
    val pending: StateFlow<Set<String>>

    /** Leaves [value] for whoever consumes [key]. A second post for the same key replaces the first. */
    fun <T : Any> post(
        key: ResultKey<T>,
        value: T,
    )

    /**
     * Takes the value and removes it: one delivery, never two.
     *
     * Throws if a value was posted under the same name with a different type, which can only mean
     * two features declared the same name — a mistake worth failing on rather than answering
     * `null` to.
     */
    fun <T : Any> consume(key: ResultKey<T>): T?

    /** Drops a pending value without delivering it — a flow abandoned rather than completed. */
    fun clear(key: ResultKey<*>)
}
