package io.thernal.navkit.navigation.api.presentation.result

import kotlin.reflect.KClass

/**
 * Names one result and fixes its type: `val SelectedPhoto = resultKey<String>("selected_photo")`.
 * Declared once, beside the producing feature's routes, and used by both sides — so a disagreement
 * about the type is a compile error.
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
