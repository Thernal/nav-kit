package io.thernal.navkit.sample.shared.arguments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.StepHeader
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.ARGUMENTS.accent

/**
 * The steps differ only in which field they touch, so they share one body. A real flow would look
 * the same: the argument is read and written through the same key on every screen.
 *
 * The field keeps its own state and writes every change through to the argument. The argument
 * store is not snapshot state, so a field whose value came from it never recomposed: every
 * keystroke was reverted, and the revert was written back as an empty string. It is read once,
 * when the step is first shown, and saved with the entry after that.
 */
@Composable
internal fun CheckoutStep(
    step: Int,
    title: String,
    label: String,
    read: (CheckoutDraft) -> String,
    write: (CheckoutDraft, String) -> CheckoutDraft,
    next: CheckoutStepRoute,
    choices: List<String> = emptyList(),
) {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    var entered by rememberSaveable { mutableStateOf(read(arguments.draft())) }
    val onEntered: (String) -> Unit = { text ->
        entered = text
        arguments.update { draft -> write(draft, text) }
    }

    SampleScreen(
        title = title,
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Continue",
                color = accent,
                enabled = entered.isNotBlank(),
                onClick = { navigator.push(next) },
            )
        },
        howItWorks = {
            Explanation(
                "Reads the shared draft and writes this one field back into it, under the same " +
                    "key on every step. Go back a step and the value is still there — the draft " +
                    "lives as long as any step of the flow is on the stack.",
            )
        },
    ) {
        StepHeader(current = step, total = CheckoutStepRoute.COUNT, label = title, accent = accent)
        if (choices.isEmpty()) {
            OutlinedTextField(
                value = entered,
                onValueChange = onEntered,
                label = { Text(text = label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(text = label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice ->
                    FilterChip(
                        selected = entered == choice,
                        onClick = { onEntered(choice) },
                        label = { Text(text = choice) },
                    )
                }
            }
        }
    }
}
