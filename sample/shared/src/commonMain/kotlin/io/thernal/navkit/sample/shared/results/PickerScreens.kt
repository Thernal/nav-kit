package io.thernal.navkit.sample.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.Amber
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.Indigo
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.Teal
import io.thernal.navkit.sample.ui.Topic

private val swatches = listOf("Teal" to Teal, "Amber" to Amber, "Indigo" to Indigo)

private fun swatchOf(name: String?): Color {
    return swatches.firstOrNull { (swatchName, _) -> swatchName == name }?.second ?: Topic.RESULTS.accent
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

@Composable
fun PickerScreen() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    SampleScreen(
        title = "Accent colour",
        topic = Topic.RESULTS,
        subtitle = "Pick one — it goes back to the screen that asked.",
        howItWorks = {
            Explanation(
                "Post, then pop: `results.post(SelectedColour, name)` followed by " +
                    "`navigator.popBack()`. The value waits in the mailbox until the screen " +
                    "underneath composes again.",
            )
        },
    ) {
        ContentCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                swatches.forEach { (name, swatch) ->
                    Swatch(
                        name = name,
                        color = swatch,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            results.post(key = SelectedColour, value = name)
                            navigator.popBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Swatch(
    name: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = name, style = MaterialTheme.typography.labelLarge)
    }
}
