package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.StatusChip

/** What became of the last link — the answer the root logged, not the one the ingress gave. */
@Composable
internal fun LinkResult(lastResult: String?) {
    ContentCard {
        val (status, color) = when {
            lastResult == null -> "No link delivered yet" to MaterialTheme.colorScheme.onSurfaceVariant
            "opened" in lastResult -> "Opened" to SamplePalette.Green
            else -> "Not opened" to SamplePalette.Rose
        }
        StatusChip(text = status, color = color)
        Text(
            text = lastResult ?: "Deliver a link to see what its handler decided.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}
