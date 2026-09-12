package io.thernal.navkit.navigation.api.presentation.host

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.model.Route

/** Registers [K] as a regular, single-pane route. */
inline fun <reified K : Route> EntryProviderScope<in K>.navEntry(
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (K) -> Unit,
) {
    entry<K>(metadata = metadata, content = content)
}
