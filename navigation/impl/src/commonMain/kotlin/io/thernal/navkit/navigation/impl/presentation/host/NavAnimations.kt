package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.SlideDirection
import io.thernal.navkit.navigation.api.presentation.transition.NavTransitionScope
import io.thernal.navkit.navigation.api.presentation.transition.NavigationDefaults
import io.thernal.navkit.navigation.api.presentation.transition.PredictiveNavTransitionScope

/**
 * The defaults a [io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams] gets
 * when it leaves a transition spec null.
 */
object NavAnimations {
    private val fadeIn = fadeIn(tween(durationMillis = NavigationDefaults.FADE_DURATION))
    private val fadeOut = fadeOut(tween(durationMillis = NavigationDefaults.FADE_DURATION))

    // An overlay draws on top of the pane it covers, so animating the pane underneath reads as the
    // whole screen sliding out from behind the sheet. Overlays get no transition at all.
    private fun <R : Route> AnimatedContentTransitionScope<Scene<R>>.involvesOverlay(): Boolean {
        return initialState is OverlayScene<*> || targetState is OverlayScene<*>
    }

    internal fun pushContentTransform(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = tween(durationMillis = duration, easing = easing),
                initialOffsetX = { width -> width / NavigationDefaults.PUSH_OFFSET_DIVIDER },
            ) + fadeIn,
            initialContentExit = slideOutHorizontally(
                animationSpec = tween(durationMillis = duration, easing = easing),
                targetOffsetX = { width -> -width / NavigationDefaults.POP_OFFSET_DIVIDER },
            ) + fadeOut,
        )
    }

    internal fun popContentTransform(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): ContentTransform {
        return ContentTransform(
            targetContentEnter = slideInHorizontally(
                animationSpec = tween(durationMillis = duration, easing = easing),
                initialOffsetX = { width -> -width / NavigationDefaults.POP_OFFSET_DIVIDER },
            ) + fadeIn,
            initialContentExit = slideOutHorizontally(
                animationSpec = tween(durationMillis = duration, easing = easing),
                targetOffsetX = { width -> width / NavigationDefaults.PUSH_OFFSET_DIVIDER },
            ) + fadeOut,
        )
    }

    fun <R : Route> push(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): NavTransitionScope<R> {
        return {
            if (involvesOverlay()) {
                noAnimation()
            } else {
                pushContentTransform(duration = duration, easing = easing)
            }
        }
    }

    fun <R : Route> pop(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): NavTransitionScope<R> {
        return {
            if (involvesOverlay()) {
                noAnimation()
            } else {
                popContentTransform(duration = duration, easing = easing)
            }
        }
    }

    fun <R : Route> predictivePop(
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): PredictiveNavTransitionScope<R> {
        return { swipeEdge ->
            if (involvesOverlay()) {
                noAnimation()
            } else {
                ContentTransform(
                    targetContentEnter = slideInHorizontally(
                        animationSpec = tween(durationMillis = duration, easing = easing),
                        initialOffsetX = { width ->
                            -width / NavigationDefaults.POP_OFFSET_DIVIDER +
                                swipeEdge / NavigationDefaults.PUSH_OFFSET_DIVIDER
                        },
                    ) + fadeIn,
                    initialContentExit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = duration, easing = easing),
                        targetOffsetX = { width ->
                            width / NavigationDefaults.PUSH_OFFSET_DIVIDER + swipeEdge / 2
                        },
                    ) + fadeOut,
                )
            }
        }
    }

    fun <R : Route> slideTo(
        direction: AnimatedContentTransitionScope<Scene<R>>.() -> SlideDirection,
        duration: Int = NavigationDefaults.DURATION,
        easing: Easing = NavigationDefaults.EASING,
    ): NavTransitionScope<R> {
        return {
            val enter: EnterTransition
            val exit: ExitTransition
            val spec = tween<IntOffset>(durationMillis = duration, easing = easing)
            when (direction()) {
                SlideDirection.LEFT -> {
                    enter = slideInHorizontally(animationSpec = spec, initialOffsetX = { width -> width })
                    exit = slideOutHorizontally(animationSpec = spec, targetOffsetX = { width -> -width })
                }

                SlideDirection.RIGHT -> {
                    enter = slideInHorizontally(animationSpec = spec, initialOffsetX = { width -> -width })
                    exit = slideOutHorizontally(animationSpec = spec, targetOffsetX = { width -> width })
                }

                SlideDirection.UP -> {
                    enter = slideInVertically(animationSpec = spec, initialOffsetY = { height -> height })
                    exit = slideOutVertically(animationSpec = spec, targetOffsetY = { height -> -height })
                }

                SlideDirection.DOWN -> {
                    enter = slideInVertically(animationSpec = spec, initialOffsetY = { height -> -height })
                    exit = slideOutVertically(animationSpec = spec, targetOffsetY = { height -> height })
                }

                SlideDirection.NONE -> {
                    enter = EnterTransition.None
                    exit = ExitTransition.None
                }
            }
            ContentTransform(
                targetContentEnter = enter + fadeIn,
                initialContentExit = exit + fadeOut,
            )
        }
    }

    private fun noAnimation(): ContentTransform {
        return ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None,
        )
    }
}
