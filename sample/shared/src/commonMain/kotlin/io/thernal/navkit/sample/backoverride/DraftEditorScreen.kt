package io.thernal.navkit.sample.backoverride

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.impl.presentation.back.NavigationBackHandler
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.Rose
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.Topic

/**
 * Intercepting back from inside a screen, for as long as it is composed.
 *
 * The callback registers against the dispatcher the mounted host provides — the same object the
 * host dispatches through — so it is heard whether back came from the system gesture or from a
 * button in a screen calling `popBack()`. Only the gesture used to ask, which made "discard unsaved
 * changes?" work in one of the two and silently not in the other.
 */
@Composable
fun DraftEditorScreen() {
    val navigator = LocalNavigator.current
    var text by rememberSaveable { mutableStateOf("") }
    var isAsking by rememberSaveable { mutableStateOf(false) }

    NavigationBackHandler(enabled = text.isNotBlank()) {
        isAsking = true
    }

    SampleScreen(
        title = "New note",
        topic = Topic.BACK_HANDLING,
        howItWorks = {
            Explanation(
                "Type something, then leave — with the system gesture or with the arrow in the top " +
                    "bar. Both end in `popBack`, which consults the back dispatcher first, so " +
                    "`NavigationBackHandler` hears both and asks.",
            )
            Explanation(
                "Discarding leaves with `popBackTo` instead, which deliberately does not consult " +
                    "the dispatcher — it is a jump, not a back — so the handler does not ask again.",
            )
        },
    ) {
        StatusChip(
            text = if (text.isBlank()) {
                "Empty — back leaves straight away"
            } else {
                "Draft · ${text.length} characters"
            },
            color = if (text.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                Rose
            },
        )
        OutlinedTextField(
            value = text,
            onValueChange = { entered -> text = entered },
            placeholder = { Text(text = "Write something…") },
            minLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (!isAsking) {
        return
    }
    AlertDialog(
        onDismissRequest = { isAsking = false },
        title = { Text(text = "Discard this note?") },
        text = { Text(text = "What you wrote will be lost.") },
        confirmButton = {
            TextButton(
                onClick = {
                    isAsking = false
                    navigator.popBackTo(inclusive = true) { route -> route is DraftEditorRoute }
                },
            ) {
                Text(text = "Discard", color = Rose)
            }
        },
        dismissButton = {
            TextButton(onClick = { isAsking = false }) {
                Text(text = "Keep writing")
            }
        },
    )
}
