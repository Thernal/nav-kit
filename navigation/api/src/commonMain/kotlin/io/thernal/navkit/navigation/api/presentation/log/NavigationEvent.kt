package io.thernal.navkit.navigation.api.presentation.log

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * Everything a navigator did, as data. Public rather than internal to `impl`: an app renders these
 * in whatever debug console it already has, and there is no way to do that from outside if the
 * type is hidden.
 */
sealed interface NavigationEvent {
    /** One line, for a list. */
    val message: String

    /** The same transition spelled out, for a detail view. */
    val detail: String

    data class Push(
        val previous: Route?,
        val pushed: Route,
    ) : NavigationEvent {
        override val message = "🧭 Push · ${previous.routeName()} -> ${pushed.routeName()}"
        override val detail = "${previous ?: "empty"} -> $pushed"
    }

    data class Pop(
        val popped: Route,
        val backTo: Route?,
    ) : NavigationEvent {
        override val message = "🧭 Pop back · ${popped.routeName()} -> ${backTo.routeName()}"
        override val detail = "$popped -> ${backTo ?: "empty"}"
    }

    data class Replace(
        val old: Route?,
        val new: Route,
    ) : NavigationEvent {
        override val message = "🧭 Replace · ${old.routeName()} -> ${new.routeName()}"
        override val detail = "${old ?: "empty"} -> $new"
    }

    data class ReplaceAll(val routes: List<Route>) : NavigationEvent {
        override val message = "🧭 Replace all · ${routes.joinToString { route -> route.routeName() }}"
        override val detail = routes.joinToString(separator = "\n") { route -> "- $route" }
    }

    /** A guard needs time to decide; [meanwhile] is what exists until it does. */
    data class Deferred(
        val attempted: ImmutableList<Route>,
        val meanwhile: ImmutableList<Route>,
    ) : NavigationEvent {
        override val message = "🧭 Deferred · ${attempted.routeNames()} (showing ${meanwhile.routeNames()})"
        override val detail = "attempted:\n${attempted.detailLines()}\n\nmeanwhile:\n${meanwhile.detailLines()}"
    }

    /**
     * A guard refused the proposed stack. Reported at stack level rather than as one rejected
     * route, because a guard decides about a transition: what it refused may be a push, a pop, or
     * a deep link's whole multi-route stack.
     */
    data class Blocked(
        val attempted: ImmutableList<Route>,
        val applied: ImmutableList<Route>,
        val reason: BlockReason?,
    ) : NavigationEvent {
        override val message = "🧭 Blocked · ${attempted.routeNames()} -> ${applied.routeNames()}" +
            reason?.let { blocked -> " (${blocked.message})" }.orEmpty()
        override val detail = "attempted:\n${attempted.detailLines()}\n\napplied:\n${applied.detailLines()}"
    }
}

private fun Route?.routeName(): String {
    return this?.let { route -> route::class.simpleName ?: route.toString() } ?: "empty"
}

private fun List<Route>.routeNames(): String {
    if (isEmpty()) {
        return "empty"
    }
    return joinToString { route -> route.routeName() }
}

private fun List<Route>.detailLines(): String {
    if (isEmpty()) {
        return "- empty"
    }
    return joinToString(separator = "\n") { route -> "- $route" }
}
