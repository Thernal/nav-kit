package io.thernal.navkit.navigation.api.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface GuardResult {
    data object Allow : GuardResult

    data class Block(val reason: String) : GuardResult

    data class Redirect(val route: Route) : GuardResult
}
