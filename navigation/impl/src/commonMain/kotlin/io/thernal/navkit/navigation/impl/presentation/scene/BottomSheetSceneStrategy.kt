package io.thernal.navkit.navigation.impl.presentation.scene

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.thernal.navkit.navigation.api.presentation.host.BOTTOM_SHEET_METADATA_KEY
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.transition.NavigationDefaults

/**
 * Claims the run of consecutive bottom-sheet entries at the top of the stack, so a sheet that pushes
 * another stays one surface with its own internal back stack.
 */
class BottomSheetSceneStrategy<R : Route> : SceneStrategy<R> {
    override fun SceneStrategyScope<R>.calculateScene(entries: List<NavEntry<R>>): Scene<R>? {
        if (entries.lastOrNull()?.metadata?.get(BOTTOM_SHEET_METADATA_KEY) != true) {
            return null
        }
        val sheetEntries = entries.takeLastWhile { entry ->
            entry.metadata[BOTTOM_SHEET_METADATA_KEY] == true
        }
        return BottomSheetScene(
            key = sheetEntries.first().contentKey,
            previousEntries = entries.dropLast(sheetEntries.size),
            overlaidEntries = entries.dropLast(sheetEntries.size),
            entries = sheetEntries,
            onBack = onBack,
        )
    }
}

private class BottomSheetScene<R : Route>(
    // `Scene.key` is `Any` in Navigation3, so the entry's own `contentKey` goes in as it comes:
    // narrowing it to `R` would cost a cast a caller-supplied `contentKey` could break.
    override val key: Any,
    override val previousEntries: List<NavEntry<R>>,
    override val overlaidEntries: List<NavEntry<R>>,
    override val entries: List<NavEntry<R>>,
    private val onBack: () -> Unit,
) : OverlayScene<R> {
    // A bare bottom-aligned container on purpose: scrim, drag handle and outside-tap dismiss belong
    // to the app's design system. Swap the content and the strategy keeps working.
    override val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.BottomCenter) {
            // The sheet's own sub-stack consumes back before the host does, so backing out of a
            // pushed sheet step returns to the previous step instead of closing the sheet.
            NavigationBackHandler(
                state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                isBackEnabled = entries.size > 1,
                onBackCompleted = onBack,
            )
            val previousSize = remember { mutableIntStateOf(entries.size) }
            val isForward = entries.size >= previousSize.intValue
            SideEffect { previousSize.intValue = entries.size }
            AnimatedContent(
                modifier = Modifier.fillMaxWidth(),
                targetState = entries.last(),
                contentKey = { entry -> entry.contentKey },
                transitionSpec = {
                    val enterOffset: (Int) -> Int = { width ->
                        if (isForward) {
                            width / NavigationDefaults.PUSH_OFFSET_DIVIDER
                        } else {
                            -width / NavigationDefaults.POP_OFFSET_DIVIDER
                        }
                    }
                    val exitOffset: (Int) -> Int = { width ->
                        if (isForward) {
                            -width / NavigationDefaults.POP_OFFSET_DIVIDER
                        } else {
                            width / NavigationDefaults.PUSH_OFFSET_DIVIDER
                        }
                    }
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = NavigationDefaults.DURATION),
                        initialOffsetX = enterOffset,
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(durationMillis = NavigationDefaults.DURATION),
                        targetOffsetX = exitOffset,
                    )
                },
            ) { entry ->
                entry.Content()
            }
        }
    }
}
