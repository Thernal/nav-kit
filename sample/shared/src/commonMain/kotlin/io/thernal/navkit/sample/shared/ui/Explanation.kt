package io.thernal.navkit.sample.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** A paragraph of the explanation; `code` and *emphasis* are rendered. */
@Composable
fun Explanation(text: String) {
    Text(
        text = rememberRichText(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
