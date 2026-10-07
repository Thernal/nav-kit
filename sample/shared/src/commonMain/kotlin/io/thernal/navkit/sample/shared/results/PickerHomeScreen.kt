package io.thernal.navkit.sample.shared.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private fun swatchOf(name: String?): Color {
    return PickerSwatches.all.firstOrNull { (swatchName, _) -> swatchName == name }?.second ?: Topic.RESULTS.accent
}

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

    SampleScreen(
        title = "Appearance",
        topic = Topic.RESULTS,
        howItWorks = {
            Explanation(
                "The picker posts the colour under a typed key and pops; this screen consumes it " +
                    "with `ResultEffect` when it is uncovered — once. The producer never learns " +
                    "who consumed it.",
            )
            Explanation(
                "`ResultEffect` reaches the mailbox through a composition local, which is what " +
                    "makes it possible at all: a composable cannot be constructor-injected. Results " +
                    "are in memory only, so a missing one is a first visit, never an error.",
            )
        },
    ) {
        HeroCard(
            title = "Ada Lovelace",
            emoji = "👩‍💻",
            accent = swatchOf(colour),
            subtitle = "Your profile, in your accent colour",
        )
        SectionLabel(text = "Theme")
        ListRow(
            title = "Accent colour",
            emoji = "🎨",
            accent = swatchOf(colour),
            subtitle = colour ?: "Not chosen yet",
            onClick = { navigator.push(PickerRoute) },
        )
    }
}
