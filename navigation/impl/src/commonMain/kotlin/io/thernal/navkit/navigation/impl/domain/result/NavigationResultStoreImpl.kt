package io.thernal.navkit.navigation.impl.domain.result

import io.thernal.navkit.navigation.api.presentation.result.NavigationResultStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

class NavigationResultStoreImpl : NavigationResultStore {
    private val mutableResults = MutableStateFlow<Map<String, Any>>(emptyMap())

    override val results: StateFlow<Map<String, Any>> = mutableResults.asStateFlow()

    override fun <T : Any> set(
        key: String,
        value: T,
    ) {
        require(key.isNotBlank()) { "Result key cannot be blank" }
        mutableResults.update { current -> current + (key to value) }
    }

    override fun <T : Any> consume(
        key: String,
        type: KClass<T>,
    ): T? {
        val value = mutableResults.value[key] ?: return null
        if (!type.isInstance(value)) {
            return null
        }
        mutableResults.update { current -> current - key }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    override fun clear(key: String) {
        mutableResults.update { current -> current - key }
    }

    override fun clearAll() {
        mutableResults.value = emptyMap()
    }
}
