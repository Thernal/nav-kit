package io.thernal.navkit.navigation.api.presentation.argument

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * The host's half of [NavigationArguments], kept off that interface so the composition local cannot
 * reach it; [pruneFor] after every stack change is what gives an argument its lifetime. **Never
 * call it from a guard**: `evaluate` must stay pure.
 */
interface ArgumentPruner {
    fun pruneFor(stack: List<Route>)
}
