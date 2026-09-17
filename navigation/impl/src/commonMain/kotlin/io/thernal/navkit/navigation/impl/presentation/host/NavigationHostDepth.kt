package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How many hosts are mounted above this one; `0` is the outermost. It exists for argument pruning:
 * the store is app-scoped while a stack is per host, so only the outermost host may prune.
 */
internal val LocalNavigationHostDepth = staticCompositionLocalOf { 0 }
