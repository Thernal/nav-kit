package io.thernal.navkit.sample.shared.results

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

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
                PickerSwatches.all.forEach { (name, swatch) ->
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
