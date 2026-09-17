package io.thernal.navkit.navigation.api.presentation.model

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey

/**
 * Marker for routes exposed across module boundaries. It extends `NavKey` so that a feature never
 * has to: a destination is declared `data object Home : Route` and Navigation3 stays a detail.
 *
 * Keep payloads small identifiers, not repositories, large models or platform objects — routes cross
 * module boundaries and often survive process death.
 */
@Immutable
interface Route : NavKey
