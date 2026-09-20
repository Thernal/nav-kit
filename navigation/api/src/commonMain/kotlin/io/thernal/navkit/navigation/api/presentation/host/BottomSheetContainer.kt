package io.thernal.navkit.navigation.api.presentation.host

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable

/**
 * The surface a bottom sheet's steps are drawn in, supplied once on the host rather than by each
 * step: they swap inside it, so the panel outlives them and its height can animate between them.
 *
 * It runs inside the host's fade, so a panel that should also move asks with `animateEnterExit`.
 * `dismiss` closes every step of the sheet at once. Unset, the host draws the steps bare.
 */
typealias BottomSheetContainer =
    @Composable AnimatedVisibilityScope.(dismiss: () -> Unit, step: @Composable () -> Unit) -> Unit
