package io.thernal.navkit.sample.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator

/**
 * The frame every example screen sits in: a top bar with back, the realistic screen in [content],
 * and below it the [howItWorks] panel that says what the example demonstrates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleScreen(
    title: String,
    topic: Topic,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showTopBar: Boolean = true,
    bottomBar: @Composable () -> Unit = {},
    howItWorks: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navigator = LocalNavigator.current
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        if (navigator.canPop()) {
                            BackArrowButton(onClick = { navigator.popBack() })
                        }
                    },
                    actions = { TopicChip(topic = topic, modifier = Modifier.padding(end = 12.dp)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            }
        },
        bottomBar = bottomBar,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
            if (howItWorks != null) {
                HowItWorks(topic = topic, content = howItWorks)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
