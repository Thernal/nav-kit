package io.thernal.navkit.navigation.api.presentation.navigator

import io.thernal.navkit.navigation.api.presentation.model.Route

// Naming aliases live outside the contract so implementations only supply the core operations.

fun Navigator.pop(): Boolean {
    return popBack()
}

fun Navigator.pop(count: Int): Boolean {
    return popBack(count)
}

fun Navigator.popTo(
    inclusive: Boolean = false,
    predicate: (Route) -> Boolean,
): Boolean {
    return popBackTo(inclusive = inclusive, predicate = predicate)
}

fun Navigator.reset(root: Route) {
    replaceAll(root)
}

fun Navigator.reset(routes: List<Route>) {
    replaceAll(routes)
}
