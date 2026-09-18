package io.thernal.navkit.navigation.api.presentation.host

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner

/**
 * The `ViewModelStoreOwner` the nearest [NavigationHost] was mounted in — the one that **outlives
 * this host's entries**.
 *
 * Inside a `navEntry`, `LocalViewModelStoreOwner` is that entry's own, so a `viewModel {}` there is
 * cleared the moment the entry leaves the stack. That is right for a pushed screen and wrong for a
 * tab: selecting another tab replaces the stack, so the tab that was showing is popped, and state
 * scoped to its entry comes back empty. Scope such state here instead, with a `key` per tab:
 *
 * ```kotlin
 * val model: SettingsTabViewModel = viewModel(
 *     viewModelStoreOwner = checkNotNull(LocalHostViewModelStoreOwner.current),
 *     key = "settings",
 * ) { SettingsTabViewModel() }
 * ```
 *
 * It resolves to the nearest host's mount point, the way [io.thernal.navkit.navigation.api
 * .presentation.navigator.LocalNavigator] resolves to the nearest host: for a nested host that is
 * the entry mounting it, for the root host the composition root. `null` outside any host, so a
 * preview composes.
 */
val LocalHostViewModelStoreOwner = staticCompositionLocalOf<ViewModelStoreOwner?> { null }
