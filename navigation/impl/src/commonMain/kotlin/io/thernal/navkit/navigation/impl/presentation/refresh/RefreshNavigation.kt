package io.thernal.navkit.navigation.impl.presentation.refresh

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Rebuilds the navigation host, for example after a language or session change. The composition
 * root supplies the callback because only it owns the back stack.
 */
val LocalRefreshNavigation = staticCompositionLocalOf<() -> Unit> {
    error("No RefreshNavigation provided")
}
