package io.thernal.navkit.navigation.api.presentation.argument

/**
 * Values travelling **forwards**: set by a screen that is opening others, read by screens that do
 * not exist yet, without being threaded through every route in between.
 *
 * It is not for results. A value handed back by a closing screen travels the other way and belongs
 * in [io.thernal.navkit.navigation.api.presentation.result.NavigationResults].
 *
 * **Lifetime is derived from the back stack**, never counted — see [ArgumentScope]. **In memory
 * only**: an argument is lost on process death while the routes that need it are restored, so a
 * consumer that finds nothing restarts its flow rather than failing. For anything that must
 * survive, put the value in a repository and only its id here.
 *
 * Put an argument in the same action that pushes the routes which read it. An argument that has
 * never been alive in the stack is kept until it is, and dropped the first time it is alive and
 * then is not. A put whose scope is already alive — a flow updating its own argument — counts as
 * alive from the start, so it still dies with the flow.
 */
interface NavigationArguments {
    /** Stores [value] for as long as [scope] says it is alive. A second put replaces the first. */
    fun <T : Any> put(
        key: ArgumentKey<T>,
        value: T,
        scope: ArgumentScope,
    )

    /**
     * Reads without removing — several screens read the same argument.
     *
     * Throws if a value was put under the same name with a different type, which can only mean two
     * features declared the same name.
     */
    fun <T : Any> get(key: ArgumentKey<T>): T?

    /** Drops an argument before its scope would. */
    fun remove(key: ArgumentKey<*>)
}
