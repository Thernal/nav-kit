package io.thernal.navkit.navigation.impl.domain.result

import io.thernal.navkit.navigation.api.presentation.result.ResultKey
import io.thernal.navkit.navigation.api.presentation.result.NavigationResults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * One posted value, kept with the key that posted it so a consumer asking under the same name with a
 * different type can be told rather than quietly handed nothing.
 */
private class PostedResult(
    val key: ResultKey<*>,
    val value: Any,
)

class NavigationResultsImpl : NavigationResults {
    private val posted = MutableStateFlow<Map<String, PostedResult>>(emptyMap())
    private val pendingNames = MutableStateFlow<Set<String>>(emptySet())

    override val pending: StateFlow<Set<String>> = pendingNames.asStateFlow()

    override fun <T : Any> post(
        key: ResultKey<T>,
        value: T,
    ) {
        posted.update { current -> current + (key.name to PostedResult(key = key, value = value)) }
        publish()
    }

    override fun <T : Any> consume(key: ResultKey<T>): T? {
        val entry = posted.value[key.name] ?: return null
        check(entry.key.type == key.type) {
            "Result `${key.name}` was posted as ${entry.key.type} and read as ${key.type}. " +
                "Two features have declared the same result name."
        }
        posted.update { current -> current - key.name }
        publish()
        @Suppress("UNCHECKED_CAST")
        return entry.value as T
    }

    override fun clear(key: ResultKey<*>) {
        posted.update { current -> current - key.name }
        publish()
    }

    /**
     * Republishes the names until they match the map they were read from: [pendingNames] only mirrors
     * [posted], so a writer that observed an older map loops rather than leaving a stale set.
     */
    private fun publish() {
        var snapshot = posted.value
        while (true) {
            pendingNames.value = snapshot.keys
            val current = posted.value
            if (current === snapshot) {
                return
            }
            snapshot = current
        }
    }
}
