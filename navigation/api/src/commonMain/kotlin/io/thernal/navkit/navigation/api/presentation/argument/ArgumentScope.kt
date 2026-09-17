package io.thernal.navkit.navigation.api.presentation.argument

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * How long an argument lives, as a question about the back stack rather than a count of who is using
 * it. (A count released on dispose does not work: Navigation3 composes only the current scene, so a
 * screen that pushes the next one leaves composition while it is still in the stack.)
 */
fun interface ArgumentScope {
    fun isAliveIn(stack: List<Route>): Boolean
}

/**
 * Alive while any route in the stack matches [predicate] — usually a whole flow at once, as in
 * `whileInStack { route -> route is CheckoutRoute }`. Scoping to a flow rather than to one screen is
 * what makes back, `popBackTo`, a guard rewrite and a deep link all come out right at once.
 */
fun whileInStack(predicate: (Route) -> Boolean): ArgumentScope {
    return ArgumentScope { stack -> stack.any(predicate) }
}

/** [whileInStack] for the common case: alive while any route of type [R] is in the stack. */
inline fun <reified R : Route> whileRouteInStack(): ArgumentScope {
    return whileInStack { route -> route is R }
}
