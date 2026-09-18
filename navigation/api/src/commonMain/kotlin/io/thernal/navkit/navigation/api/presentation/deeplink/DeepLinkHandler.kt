package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkRequest

/**
 * Resolves the pages one feature owns. Each page has exactly one owning handler — a duplicate is a
 * startup error, not a silent last-one-wins.
 */
interface DeepLinkHandler {
    val pages: Set<String>

    suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome
}
