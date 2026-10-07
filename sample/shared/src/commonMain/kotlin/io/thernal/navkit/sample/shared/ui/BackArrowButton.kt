package io.thernal.navkit.sample.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/** A back arrow drawn rather than imported: the sample carries no icon set. */
@Composable
fun BackArrowButton(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurface
    IconButton(onClick = onClick) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.dp.toPx()
            val middle = Offset(x = size.width * 0.12f, y = size.height / 2)
            drawLine(
                color = color,
                start = Offset(x = size.width * 0.9f, y = size.height / 2),
                end = middle,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = middle,
                end = Offset(x = size.width * 0.45f, y = size.height * 0.18f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = middle,
                end = Offset(x = size.width * 0.45f, y = size.height * 0.82f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
