package io.thernal.navkit.navigation.api.presentation.argument

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * The host's half of [NavigationArguments], kept off that interface so the composition local every
 * screen reads cannot reach it. The mounted host calls [pruneFor] after every stack change, which is
 * the only thing that gives an argument its lifetime.
 *
 * **Never call it from a guard**: `NavigationGuard.evaluate` must stay pure and runs several times
 * per navigation, so pruning there deletes arguments while the stack is still being decided.
 */
interface ArgumentPruner {
    fun pruneFor(stack: List<Route>)
}
