package io.thernal.navkit.navigation.api.presentation.deeplink

import io.thernal.navkit.navigation.api.domain.DeepLinkPage
import kotlin.enums.EnumEntries

/** Derives the handled page set from an enum of [DeepLinkPage] entries, so only `resolve` is left. */
abstract class TypedDeepLinkHandler<T>(entries: EnumEntries<T>) :
    DeepLinkHandler
    where T : Enum<T>, T : DeepLinkPage {
    final override val pages: Set<String> = entries.map { entry -> entry.page }.toSet()
}
