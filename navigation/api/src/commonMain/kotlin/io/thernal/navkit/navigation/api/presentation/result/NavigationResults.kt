package io.thernal.navkit.navigation.api.presentation.result

import kotlinx.coroutines.flow.StateFlow

/**
 * A keyed mailbox for values travelling **backwards**, from a closing screen to one already in the
 * stack. Forwards is
 * [NavigationArguments][io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments].
 * **In memory only**, so a missing result is a first visit, never an error.
 */
interface NavigationResults {
    /** The names with a value waiting — names, so one feature's results are not readable by every screen. */
    val pending: StateFlow<Set<String>>

    /** Leaves [value] for whoever consumes [key]. A second post for the same key replaces the first. */
    fun <T : Any> post(
        key: ResultKey<T>,
        value: T,
    )

    /**
     * Takes the value and removes it: one delivery, never two. Throws if it was posted under the
     * same name with a different type, which can only mean two features chose one name.
     */
    fun <T : Any> consume(key: ResultKey<T>): T?

    /** Drops a pending value without delivering it — a flow abandoned rather than completed. */
    fun clear(key: ResultKey<*>)
}
