package io.thernal.navkit.sample.shared.backoverride

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.sample.shared.ui.DemoButton
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.PrimaryButton
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.StatusChip
import io.thernal.navkit.sample.shared.ui.Topic

@Composable
fun ArticleEditorScreen(drafts: ArticleDraftStore) {
    val navigator = LocalNavigator.current
    var lastRefusal by rememberSaveable { mutableStateOf("—") }

    SampleScreen(
        title = "Editor",
        topic = Topic.BACK_HANDLING,
        howItWorks = {
            LiveValue(label = "Last attempt to leave", value = lastRefusal)
            Explanation(
                "Type, then try every way out: the back arrow, the gesture, and the two demo jumps " +
                    "below. `UnsavedWorkGuard` compares the stack before and after and refuses any " +
                    "change that drops the editor while the body differs from what was saved.",
            )
            Explanation(
                "The refusal carries the application's own `BlockReason`, which comes back on the " +
                    "command's `NavigationOutcome`. The navigation layer never decides whether that " +
                    "reads as a prompt, a paywall or an error.",
            )
        },
    ) {
        StatusChip(
            text = if (drafts.hasUnsavedChanges) {
                "Unsaved changes"
            } else {
                "All changes saved"
            },
            color = if (drafts.hasUnsavedChanges) {
                SamplePalette.Rose
            } else {
                SamplePalette.Green
            },
        )
        OutlinedTextField(
            value = drafts.body,
            onValueChange = { entered -> drafts.edit(entered) },
            label = { Text(text = "Why back stacks are state") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                label = "Discard",
                enabled = drafts.hasUnsavedChanges,
                modifier = Modifier.weight(1f),
                onClick = {
                    drafts.discard()
                    lastRefusal = "discarded — the guard will let go now"
                },
            )
            PrimaryButton(
                label = "Save",
                enabled = drafts.hasUnsavedChanges,
                modifier = Modifier.weight(1f),
                onClick = {
                    drafts.save()
                    lastRefusal = "saved — the guard will let go now"
                },
            )
        }
        SectionLabel(text = "Try to leave another way")
        DemoButton(
            label = "Jump home · popBackTo",
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
        DemoButton(
            label = "Reset the stack · replaceAll",
            onClick = {
                val outcome = navigator.replaceAll(ArticleHomeRoute)
                lastRefusal = when (outcome) {
                    is NavigationOutcome.Applied -> "left the editor"
                    is NavigationOutcome.Rewritten -> "refused: ${outcome.reason?.message ?: "no reason"}"
                    is NavigationOutcome.Deferred -> "a guard needs time"
                }
            },
        )
    }
}
