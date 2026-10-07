package io.thernal.navkit.sample.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** A topic's pill: its emoji and its name, in its accent colour. */
@Composable
fun TopicChip(
    topic: Topic,
    modifier: Modifier = Modifier,
) {
    StatusChip(
        text = "${topic.emoji}  ${topic.label}",
        color = topic.accent,
        modifier = modifier,
    )
}
