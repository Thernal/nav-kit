package io.thernal.navkit.sample.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.host.BottomSheetContainer
import io.thernal.navkit.navigation.api.presentation.transition.NavigationDefaults

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

private val ScrimColour = Color(0x66000000)

/** The application's sheet surface, installed once on the root host. */
val SampleSheetContainer: BottomSheetContainer = { dismiss, step ->
    SheetSurface(dismiss = dismiss, content = step)
}

/**
 * Everything a sheet looks like — the kit draws none of it, and this is the surface for every step
 * rather than one of them.
 *
 * It fills the window: an overlay is a sibling of the pane, so a panel that measured only as tall
 * as itself would land against the top of the screen.
 */
@Composable
fun AnimatedVisibilityScope.SheetSurface(
    dismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScrimColour)
            // No indication: the scrim is a dismiss area, not a button.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = dismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // The host's fade covers the scrim; the panel is the part that should also move.
                .animateEnterExit(
                    enter = slideInVertically(animationSpec = sheetSlide()) { height -> height },
                    exit = slideOutVertically(animationSpec = sheetSlide()) { height -> height },
                )
                // A `Surface` does not consume taps; without this they reach the scrim and dismiss.
                .pointerInput(Unit) { detectTapGestures { } },
            shape = SheetShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DragHandle()
                content()
            }
        }
    }
}

/** One step inside the surface: its heading travels with its body when the next step arrives. */
@Composable
fun SheetStep(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        content()
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

private fun sheetSlide(): FiniteAnimationSpec<IntOffset> {
    return tween(
        durationMillis = NavigationDefaults.DURATION,
        easing = NavigationDefaults.EASING,
    )
}
