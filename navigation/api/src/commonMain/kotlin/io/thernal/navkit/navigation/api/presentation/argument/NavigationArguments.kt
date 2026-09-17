package io.thernal.navkit.navigation.api.presentation.argument

/**
 * Values travelling **forwards**: set by a screen that is opening others, read by screens that do
 * not exist yet, without being threaded through every route in between. A value handed back by a
 * closing screen travels the other way and belongs in
 * [io.thernal.navkit.navigation.api.presentation.result.NavigationResults].
 *
 * Lifetime is derived from the back stack, never counted — see [ArgumentScope]. **In memory only**:
 * an argument is lost on process death while the routes that need it are restored, so a consumer
 * that finds nothing restarts its flow rather than failing.
 *
 * Put an argument in the same action that pushes the routes which read it. See
 * `navigation/README.md`, "Arguments — forwards".
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
