package io.thernal.navkit.navigation.api.presentation.argument

import kotlin.reflect.KClass

/**
 * Names one forward argument and fixes its type, declared next to the routes of the flow that
 * reads it.
 *
 * ```kotlin
 * val CheckoutDraft = argumentKey<Draft>("checkout_draft")
 * ```
 */
data class ArgumentKey<T : Any>(
    val name: String,
    val type: KClass<T>,
) {
    init {
        require(name.isNotBlank()) { "An argument key needs a name" }
    }
}

inline fun <reified T : Any> argumentKey(name: String): ArgumentKey<T> {
    return ArgumentKey(name = name, type = T::class)
}
