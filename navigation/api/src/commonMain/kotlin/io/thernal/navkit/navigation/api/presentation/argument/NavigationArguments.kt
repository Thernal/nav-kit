package io.thernal.navkit.navigation.api.presentation.argument

/**
 * Values travelling **forwards**, to screens that do not exist yet. Backwards is
 * [NavigationResults][io.thernal.navkit.navigation.api.presentation.result.NavigationResults].
 * Lifetime comes from the back stack — see [ArgumentScope] — and is **in memory only**, so a
 * consumer that finds nothing restarts its flow.
 */
interface NavigationArguments {
    /** Stores [value] for as long as [scope] says it is alive. A second put replaces the first. */
    fun <T : Any> put(
        key: ArgumentKey<T>,
        value: T,
        scope: ArgumentScope,
    )

    /**
     * Reads without removing — several screens read the same argument. Throws if the value was put
     * under the same name with a different type, which can only mean two features chose one name.
     */
    fun <T : Any> get(key: ArgumentKey<T>): T?

    /** Drops an argument before its scope would. */
    fun remove(key: ArgumentKey<*>)
}
