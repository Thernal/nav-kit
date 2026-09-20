package io.thernal.navkit.navigation.impl.presentation.scene.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.presentation.host.NavAnimations

/**
 * The step showing inside the sheet, and how it gives way to the next.
 *
 * `ContentTransform` animates size too, which inside a panel that stays put is the sheet's height.
 */
@Composable
internal fun <R : Route> BottomSheetSteps(steps: List<NavEntry<R>>) {
    val previousSize = remember { mutableIntStateOf(steps.size) }
    val isForward = steps.size >= previousSize.intValue
    SideEffect { previousSize.intValue = steps.size }

    AnimatedContent(
        targetState = steps.last(),
        modifier = Modifier.fillMaxWidth(),
        contentKey = { entry -> entry.contentKey },
        transitionSpec = {
            if (isForward) {
                NavAnimations.pushContentTransform()
            } else {
                NavAnimations.popContentTransform()
            }
        },
    ) { entry ->
        entry.Content()
    }
}
