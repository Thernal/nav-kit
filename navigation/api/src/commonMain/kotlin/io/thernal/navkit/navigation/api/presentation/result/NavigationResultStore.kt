package io.thernal.navkit.navigation.api.presentation.result

import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/** Passes a value back from one screen to another without a shared ViewModel. */
interface NavigationResultStore {
    val results: StateFlow<Map<String, Any>>

    fun <T : Any> set(
        key: String,
        value: T,
    )

    /**
     * [KClass] rather than the `Class<T>` the JVM-only original took — `KClass.isInstance` is the
     * one runtime type check the common stdlib actually offers.
     */
    fun <T : Any> consume(
        key: String,
        type: KClass<T>,
    ): T?

    fun clear(key: String)

    fun clearAll()
}

inline fun <reified T : Any> NavigationResultStore.consume(key: String): T? {
    return consume(key = key, type = T::class)
}
