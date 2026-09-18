package io.thernal.navkit.sample.backoverride

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleScaffold

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

    ExampleScaffold(
        title = "Draft",
        subtitle = "Type something, then try to leave — with the gesture or with the button.",
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { entered -> text = entered },
            label = { Text(text = "Draft") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleNote(
            text = "The scaffold's Back button calls `popBack`, which consults the dispatcher " +
                "first — so it is intercepted too. Discarding uses `popBackTo` instead, which " +
                "deliberately does not consult it: that is a jump, not a back.",
        )
    }

    if (!isAsking) {
        return
    }
    AlertDialog(
        onDismissRequest = { isAsking = false },
        title = { Text(text = "Discard the draft?") },
        text = { Text(text = "What you typed will be lost.") },
        confirmButton = {
            TextButton(
                onClick = {
                    isAsking = false
                    navigator.popBackTo(inclusive = true) { route -> route is DraftEditorRoute }
                },
            ) {
                Text(text = "Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = { isAsking = false }) {
                Text(text = "Keep editing")
            }
        },
    )
}
