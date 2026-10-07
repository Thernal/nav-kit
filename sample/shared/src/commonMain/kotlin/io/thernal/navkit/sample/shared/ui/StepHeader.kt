package io.thernal.navkit.sample.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** "Step 2 of 3" with a progress bar, for multi-step flows. */
@Composable
fun StepHeader(
    current: Int,
    total: Int,
    label: String,
    accent: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "STEP $current OF $total",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { current.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = accent,
            trackColor = accent.copy(alpha = 0.15f),
        )
    }
}
