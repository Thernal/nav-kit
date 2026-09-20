package io.thernal.navkit.navigation.impl.presentation.scene.bottomsheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import io.thernal.navkit.navigation.api.presentation.host.BottomSheetContainer
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.transition.NavigationDefaults
import kotlinx.coroutines.flow.first

/** One open sheet: the steps in it and the fade it arrives on. It draws nothing itself. */
internal class BottomSheetScene<R : Route>(
    // `Scene.key` is `Any` in Navigation3, so the entry's own `contentKey` goes in as it comes:
    // narrowing it to `R` would cost a cast a caller-supplied `contentKey` could break.
    override val key: Any,
    initialSteps: List<NavEntry<R>>,
    initialBelow: List<NavEntry<R>>,
    private val container: BottomSheetContainer?,
    private val onClosed: (Any) -> Unit,
    private val onBack: () -> Unit,
) : OverlayScene<R> {
    // Snapshot state: the scene outlives the stack changes inside its own run.
    private var steps by mutableStateOf(initialSteps)

    private var below by mutableStateOf(initialBelow)

    // Not remembered in the content: `onRemove` runs outside composition and has to await it.
    private val visibility = MutableTransitionState(initialState = false).apply { targetState = true }

    val isClosing: Boolean
        get() = !visibility.targetState

    override val entries: List<NavEntry<R>>
        get() = steps

    override val previousEntries: List<NavEntry<R>>
        get() = below

    override val overlaidEntries: List<NavEntry<R>>
        get() = below

    override val content: @Composable () -> Unit = {
        AnimatedVisibility(
            visibleState = visibility,
            modifier = Modifier
                .fillMaxWidth()
                .ignorePointerInput(isIgnored = isClosing),
            enter = fadeIn(animationSpec = sheetFade()),
            exit = fadeOut(animationSpec = sheetFade()),
        ) {
            // Consumed before the host's, so a pushed step returns instead of closing the sheet.
            NavigationBackHandler(
                state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                isBackEnabled = steps.size > 1,
                onBackCompleted = onBack,
            )
            val currentSteps = steps
            if (container == null) {
                BottomSheetSteps(steps = currentSteps)
            } else {
                container(rememberDismiss()) { BottomSheetSteps(steps = currentSteps) }
            }
        }
    }

    override suspend fun onRemove() {
        visibility.targetState = false
        snapshotFlow { visibility.isIdle && !visibility.currentState }.first { isGone -> isGone }
        onClosed(this)
    }

    fun moveTo(
        steps: List<NavEntry<R>>,
        below: List<NavEntry<R>>,
    ) {
        this.steps = steps
        this.below = below
    }

    /** One command over the whole run. */
    @Composable
    private fun rememberDismiss(): () -> Unit {
        val navigator = LocalNavigator.current
        return remember(navigator) {
            { navigator.popBack(count = steps.size) }
        }
    }
}

private fun sheetFade(): FiniteAnimationSpec<Float> {
    return tween(
        durationMillis = NavigationDefaults.DURATION,
        easing = NavigationDefaults.EASING,
    )
}
