package io.thernal.navkit.navigation.api.presentation.transition

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

object NavigationDefaults {
    const val DURATION = 260
    const val FADE_DURATION = 180
    const val PUSH_OFFSET_DIVIDER = 3
    const val POP_OFFSET_DIVIDER = 6
    val EASING: Easing = FastOutSlowInEasing
}
