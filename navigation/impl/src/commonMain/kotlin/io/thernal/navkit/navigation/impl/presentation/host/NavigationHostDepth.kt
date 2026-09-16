package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How many hosts are already mounted above this one. `0` is the outermost host.
 *
 * Only one thing needs it today, and it is the reason it exists: the argument store is one
 * application-scoped object while a stack is per host, so a nested host pruning against its own
 * stack would delete the arguments of the flow that mounted it. The outermost host owns the prune.
 */
internal val LocalNavigationHostDepth = staticCompositionLocalOf { 0 }
