package io.thernal.navkit.navigation.api.presentation.log

import io.thernal.navkit.navigation.api.presentation.model.Route

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

    data class Blocked(
        val route: Route,
        val reason: String,
    ) : NavigationEvent {
        override val message = "🧭 Blocked · ${route.routeName()} ($reason)"
        override val detail = "$route blocked: $reason"
    }
}

private fun Route?.routeName(): String {
    return this?.let { route -> route::class.simpleName ?: route.toString() } ?: "empty"
}
