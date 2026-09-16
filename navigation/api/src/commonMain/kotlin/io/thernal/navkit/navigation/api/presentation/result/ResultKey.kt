package io.thernal.navkit.navigation.api.presentation.result

import kotlin.reflect.KClass

/**
 * Names one result and fixes its type.
 *
 * Declared once, next to the routes of the feature that produces the result, and used by both
 * sides — which is what makes a producer and a consumer disagreeing about the type a compile error
 * rather than a `null` nobody notices.
 *
 * ```kotlin
 * val SelectedPhoto = resultKey<String>("selected_photo")
 * ```
 */
data class ResultKey<T : Any>(
    val name: String,
    val type: KClass<T>,
) {
    init {
        require(name.isNotBlank()) { "A result key needs a name" }
    }
}

inline fun <reified T : Any> resultKey(name: String): ResultKey<T> {
    return ResultKey(name = name, type = T::class)
}
