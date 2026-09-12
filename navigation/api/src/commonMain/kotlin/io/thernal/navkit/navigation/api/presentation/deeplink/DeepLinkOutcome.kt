package io.thernal.navkit.navigation.api.presentation.deeplink

import androidx.compose.runtime.Immutable
import io.thernal.navkit.navigation.api.presentation.model.Route

/** What a handler decided to do with a request. */
@Immutable
sealed interface DeepLinkOutcome {
    data class Navigate(val routes: List<Route>) : DeepLinkOutcome

    data class Rejected(val reason: String) : DeepLinkOutcome

    data object NotFound : DeepLinkOutcome
}
