package io.thernal.navkit.navigation.api.presentation.model

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey

/**
 * Marker for routes exposed across module boundaries.
 *
 * Keep payloads small identifiers, not repositories, large models, or platform objects — routes
 * cross module boundaries and often survive process death.
 */
@Immutable
interface Route : NavKey
