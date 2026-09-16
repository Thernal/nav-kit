package io.thernal.navkit.sample.backoverride

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

@Composable
fun ArticleHomeScreen(drafts: ArticleDraftStore) {
    val navigator = LocalNavigator.current
    val body by drafts.body.collectAsState()

    ExampleScaffold(
        title = "Articles",
        subtitle = "The editor cannot be left with unsaved work — enforced by a guard, not by a screen.",
    ) {
        ExampleReadout(label = "Saved body", value = body.ifBlank { "—" })
        ExampleAction(
            label = "Open the editor",
            onClick = { navigator.push(ArticleEditorRoute) },
        )
        ExampleNote(
            text = "A `NavigationBackHandler` only fires while its screen is composed, so it " +
                "cannot stop a jump that skips the screen — a deep link, a `replaceAll`, a guard " +
                "rewrite. A transition guard can, because it sees every stack change.",
        )
    }
}

@Composable
fun ArticleEditorScreen(drafts: ArticleDraftStore) {
    val navigator = LocalNavigator.current
    val body by drafts.body.collectAsState()
    var lastRefusal by remember { mutableStateOf("—") }

    ExampleScaffold(
        title = "Editor",
        subtitle = "Every way out is refused while the body differs from what was saved.",
    ) {
        OutlinedTextField(
            value = body,
            onValueChange = { entered -> drafts.edit(entered) },
            label = { Text(text = "Body") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleReadout(
            label = "Unsaved changes",
            value = drafts.hasUnsavedChanges.toString(),
        )
        ExampleReadout(label = "Last attempt", value = lastRefusal)

        ExampleAction(
            label = "Try to jump home (popBackTo)",
            onClick = {
                // `popBackTo` does not consult the back dispatcher, so a screen-level handler would
                // never see this. The guard does.
                val didMove = navigator.popBackTo { route -> route is ArticleHomeRoute }
                lastRefusal = if (didMove) {
                    "left the editor"
                } else {
                    "refused: ${UnsavedWork.message}"
                }
            },
        )
        ExampleAction(
            label = "Try to reset the stack (replaceAll)",
            onClick = {
                val outcome = navigator.replaceAll(ArticleHomeRoute)
                lastRefusal = when (outcome) {
                    is NavigationOutcome.Applied -> "left the editor"
                    is NavigationOutcome.Rewritten -> "refused: ${outcome.reason?.message ?: "no reason"}"
                    is NavigationOutcome.Deferred -> "a guard needs time"
                }
            },
        )
        ExampleAction(
            label = "Save",
            onClick = {
                drafts.save()
                lastRefusal = "saved — the guard will let go now"
            },
            enabled = drafts.hasUnsavedChanges,
        )
        ExampleAction(
            label = "Discard",
            onClick = {
                drafts.discard()
                lastRefusal = "discarded — the guard will let go now"
            },
            enabled = drafts.hasUnsavedChanges,
        )
        ExampleNote(
            text = "The refusal carries the application's own `BlockReason`, which comes back on " +
                "the command's `NavigationOutcome`. The navigation layer never decides whether " +
                "that reads as a prompt, a paywall or an error.",
        )
    }
}
