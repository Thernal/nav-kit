package io.thernal.navkit.sample.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A primary action pinned to the bottom of the screen, as checkout and form screens have. */
@Composable
fun BottomAction(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                PrimaryButton(
                    label = label,
                    onClick = onClick,
                    enabled = enabled,
                    color = color,
                )
            }
        }
    }
}
