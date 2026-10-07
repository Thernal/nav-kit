package io.thernal.navkit.sample.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample
import io.thernal.navkit.sample.ui.IconBadge
import io.thernal.navkit.sample.ui.Indigo
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.Teal
import io.thernal.navkit.sample.ui.Topic
import kotlinx.collections.immutable.ImmutableList

/**
 * The index. It knows nothing about any example: the list arrives from the graph, so adding an
 * example never touches this file.
 */
@Composable
fun CatalogScreen(examples: ImmutableList<SampleExample>) {
    val navigator = LocalNavigator.current
    val byTopic = remember(examples) { examples.groupBy { example -> example.topic } }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CatalogHeader(exampleCount = examples.size, topicCount = byTopic.size)
            }
            byTopic.forEach { (topic, topicExamples) ->
                item(key = topic.name) {
                    TopicHeader(topic = topic)
                }
                items(items = topicExamples, key = { example -> example.title }) { example ->
                    CatalogRow(
                        example = example,
                        onOpen = { navigator.push(example.route) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(
    exampleCount: Int,
    topicCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.linearGradient(colors = listOf(Indigo, Teal)))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "🧭", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "nav-kit",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Text(
            text = "Navigation3 patterns for Compose Multiplatform — each one as a simple example " +
                "and as an advanced one, the shape a production screen takes.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
        Text(
            text = "$topicCount capabilities · $exampleCount examples",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
        )
    }
}

@Composable
private fun TopicHeader(topic: Topic) {
    Row(
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(emoji = topic.emoji, color = topic.accent, size = 32.dp)
        Text(text = topic.label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CatalogRow(
    example: SampleExample,
    onOpen: () -> Unit,
) {
    val accent = example.topic.accent
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusChip(
                    text = example.kind.label.uppercase(),
                    color = if (example.kind == ExampleKind.ADVANCED) {
                        accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(text = example.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = example.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
            )
        }
    }
}

/** Kind ordering puts the simple example of a group before its advanced sibling. */
internal fun ExampleKind.sortKey(): Int {
    return when (this) {
        ExampleKind.SIMPLE -> 0
        ExampleKind.ADVANCED -> 1
    }
}
