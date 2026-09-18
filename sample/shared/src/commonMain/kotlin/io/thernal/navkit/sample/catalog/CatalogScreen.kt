package io.thernal.navkit.sample.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample
import kotlinx.collections.immutable.ImmutableList

/**
 * The index. It knows nothing about any example: the list arrives from the graph, so adding an
 * example never touches this file.
 */
@Composable
fun CatalogScreen(examples: ImmutableList<SampleExample>) {
    val navigator = LocalNavigator.current
    Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "nav-kit", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "One simple and one real-life example per capability.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(examples) { example ->
                CatalogRow(
                    example = example,
                    onOpen = { navigator.push(example.route) },
                )
            }
        }
    }
}

@Composable
private fun CatalogRow(
    example: SampleExample,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${example.group} · ${example.kind.label}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = example.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = example.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Kind ordering puts the simple example of a group before its real-life sibling. */
internal fun ExampleKind.sortKey(): Int {
    return when (this) {
        ExampleKind.SIMPLE -> 0
        ExampleKind.REAL_LIFE -> 1
    }
}
