package io.thernal.navkit.sample.backoverride

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The editor's state, held outside the composition so a guard can read it.
 *
 * That is the whole reason it exists as an object rather than as `remember` inside the screen: a
 * guard runs before a stack reaches the host, never during composition, so it cannot see anything a
 * composable is holding.
 */
class ArticleDraftStore {
    private val mutableBody = MutableStateFlow("")
    private val mutableSaved = MutableStateFlow("")

    val body: StateFlow<String> = mutableBody.asStateFlow()

    val hasUnsavedChanges: Boolean
        get() = mutableBody.value != mutableSaved.value

    fun edit(next: String) {
        mutableBody.value = next
    }

    fun save() {
        mutableSaved.value = mutableBody.value
    }

    fun discard() {
        mutableBody.value = mutableSaved.value
    }
}
