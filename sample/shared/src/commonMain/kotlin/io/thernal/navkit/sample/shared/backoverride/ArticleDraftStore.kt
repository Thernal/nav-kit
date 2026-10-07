package io.thernal.navkit.sample.backoverride

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The editor's state, held outside the composition so a guard can read it.
 *
 * That is the whole reason it exists as an object rather than as `remember` inside the screen: a
 * guard runs before a stack reaches the host, never during composition, so it cannot see anything a
 * composable is holding.
 *
 * Snapshot state rather than a `StateFlow`, because a text field is bound to [body]. A flow collected
 * into composition hands the field its value a frame late, and a field that is behind the keyboard
 * drops characters and moves the cursor when typing is fast. Snapshot state is read and written
 * synchronously, and [hasUnsavedChanges] is observable for free.
 */
class ArticleDraftStore {
    var body: String by mutableStateOf("")
        private set

    var saved: String by mutableStateOf("")
        private set

    val hasUnsavedChanges: Boolean
        get() {
            return body != saved
        }

    fun edit(next: String) {
        body = next
    }

    fun save() {
        saved = body
    }

    fun discard() {
        body = saved
    }
}
