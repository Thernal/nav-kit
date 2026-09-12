package io.thernal.navkit.navigation.api.presentation.host

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.model.Route

/** [androidx.navigation3.runtime.NavEntry] metadata key the installed [NavigationHost] checks. */
const val BOTTOM_SHEET_METADATA_KEY = "nav-bottom-sheet"

/** [androidx.navigation3.runtime.NavEntry] metadata key the installed [NavigationHost] checks. */
const val MODAL_METADATA_KEY = "nav-modal"

/** Registers [K] to render inside a bottom sheet instead of the primary pane. */
inline fun <reified K : Route> EntryProviderScope<in K>.bottomSheetEntry(noinline content: @Composable (K) -> Unit) {
    entry<K>(
        metadata = mapOf(BOTTOM_SHEET_METADATA_KEY to true),
        content = content,
    )
}

/** Registers [K] to render inside a modal instead of the primary pane. */
inline fun <reified K : Route> EntryProviderScope<in K>.modalEntry(noinline content: @Composable (K) -> Unit) {
    entry<K>(
        metadata = mapOf(MODAL_METADATA_KEY to true),
        content = content,
    )
}
