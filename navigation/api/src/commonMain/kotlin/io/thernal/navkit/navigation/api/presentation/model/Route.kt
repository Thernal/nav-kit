package io.thernal.navkit.navigation.api.presentation.model

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey

/**
 * Marker for routes exposed across module boundaries; it extends `NavKey` so a feature never has to.
 * Keep payloads small identifiers — routes cross modules and often survive process death.
 */
@Immutable
interface Route : NavKey
