package io.thernal.navkit.navigation.impl.domain.navigator

import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEvent
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.GuardResult
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator

/**
 * Adapts a caller-owned back stack into the [Navigator] command surface. [buildStack] is the one
 * primitive every other command goes through, mutating the receiver [buildBackStack] hands it
 * directly — no separate read/transform/write step. Holds no state of its own: `NavigationView`
 * builds one per host, over whatever backs `NavigationHostParams.backStack`.
 *
 * `buildBackStack`/`resolveCanPop` are named apart from the [buildStack]/[canPop] members they
 * back — a same-named property and override recurse into each other instead of resolving to the
 * constructor value.
 *
 * Every command that can add a new route to the stack ([push], [navigate], [replace],
 * [replaceAll]) resolves it through [guardRunner] first, before the stack is touched — a blocked
 * route never reaches the stack, so it never renders. [popBack]/[popBackTo] only remove routes
 * that already passed a guard when they were added, so they run unguarded.
 */
class BackStackNavigator(
    private val buildBackStack: (MutableList<Route>.() -> Unit) -> Unit,
    private val resolveCanPop: () -> Boolean,
    private val guardRunner: NavigationGuardRunner,
    private val events: NavigationEventSink = NavigationEventSink.NoOp,
) : Navigator {

    override fun buildStack(builder: MutableList<Route>.() -> Unit) {
        buildBackStack(builder)
    }

    override fun canPop(): Boolean {
        return resolveCanPop()
    }

    override fun push(route: Route) {
        val resolved = resolve(route) ?: return
        pushUnchecked(resolved)
    }

    override fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)?,
    ) {
        val resolved = resolve(route) ?: return
        if (!popBackTo(predicate = predicate ?: { candidate -> candidate == resolved })) {
            pushUnchecked(resolved)
        }
    }

    override fun replace(route: Route) {
        val resolved = resolve(route) ?: return
        buildStack {
            events.emit(NavigationEvent.Replace(old = lastOrNull(), new = resolved))
            if (isNotEmpty()) {
                removeLastOrNull()
            }
            add(resolved)
        }
    }

    override fun replaceAll(routes: List<Route>) {
        require(routes.isNotEmpty()) { "Navigation back stack cannot be empty" }
        val resolvedLast = resolve(routes.last()) ?: return
        val resolvedRoutes = routes.dropLast(1) + resolvedLast
        buildStack {
            events.emit(NavigationEvent.ReplaceAll(resolvedRoutes))
            clear()
            addAll(resolvedRoutes)
        }
    }

    override fun popBack(force: Boolean): Boolean {
        var didChange = false
        buildStack {
            if (size < 2 && !force) {
                return@buildStack
            }
            val popped = lastOrNull() ?: return@buildStack
            removeLastOrNull()
            events.emit(NavigationEvent.Pop(popped = popped, backTo = lastOrNull()))
            didChange = true
        }
        return didChange
    }

    override fun popBack(count: Int): Boolean {
        require(count >= 0) { "count must be non-negative" }
        var didChange = false
        repeat(count) { didChange = popBack() || didChange }
        return didChange
    }

    override fun popBackTo(
        inclusive: Boolean,
        predicate: (Route) -> Boolean,
    ): Boolean {
        var didPop = false
        buildStack {
            val index = indexOfLast(predicate)
            if (index == -1) {
                return@buildStack
            }
            val endExclusive = if (inclusive) {
                index
            } else {
                index + 1
            }
            val next = take(endExclusive)
            if (next.isEmpty()) {
                return@buildStack
            }
            if (next.size != size) {
                events.emit(NavigationEvent.ReplaceAll(next))
            }
            clear()
            addAll(next)
            didPop = true
        }
        return didPop
    }

    private fun pushUnchecked(route: Route) {
        buildStack {
            events.emit(NavigationEvent.Push(previous = lastOrNull(), pushed = route))
            add(route)
        }
    }

    /** `Allow` keeps [route], `Redirect` substitutes its target, `Block` reports and yields `null`. */
    private fun resolve(route: Route): Route? {
        return when (val result = guardRunner.evaluate(route)) {
            GuardResult.Allow -> route

            is GuardResult.Redirect -> result.route

            is GuardResult.Block -> {
                events.emit(NavigationEvent.Blocked(route = route, reason = result.reason))
                null
            }
        }
    }
}
