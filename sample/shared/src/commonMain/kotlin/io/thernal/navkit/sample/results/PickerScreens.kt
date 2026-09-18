package io.thernal.navkit.sample.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

/**
 * A value travelling **backwards**: the screen that produces it is closing, the screen that wants
 * it is already in the stack and about to be uncovered.
 */
@Composable
fun PickerHomeScreen() {
    val navigator = LocalNavigator.current
    // Saveable, not remembered: this screen leaves composition whenever the picker covers it, and
    // a plain `remember` forgot the last pick every time the picker was opened again.
    var colour by rememberSaveable { mutableStateOf<String?>(null) }

    // Navigation3 composes only the entries of the current scene, so this screen is not composed
    // while the picker covers it and the effect does not run. It runs when the user comes back —
    // which is exactly when a returning result is wanted.
    ResultEffect(SelectedColour) { picked -> colour = picked }

    ExampleScaffold(
        title = "Pick a colour",
        subtitle = "The picker posts; this screen consumes, once.",
    ) {
        ExampleReadout(label = "Selected", value = colour ?: "nothing yet")
        ExampleAction(
            label = "Open the picker",
            onClick = { navigator.push(PickerRoute) },
        )
        ExampleNote(
            text = "`ResultEffect` reaches the mailbox through a composition local, which is what " +
                "makes it possible at all: a composable cannot be constructor-injected. Consuming " +
                "removes the value — one delivery, never two.",
        )
    }
}

@Composable
fun PickerScreen() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    ExampleScaffold(
        title = "Picker",
        subtitle = "Post, then pop. The producer never learns who consumed it.",
    ) {
        listOf("Teal", "Amber", "Indigo").forEach { colour ->
            ExampleAction(
                label = colour,
                onClick = {
                    results.post(key = SelectedColour, value = colour)
                    navigator.popBack()
                },
            )
        }
        ExampleNote(
            text = "Results are in memory only. A posted value is lost on process death while the " +
                "routes that would have consumed it are restored, so a consumer treats a missing " +
                "result as a first visit rather than an error.",
        )
    }
}
