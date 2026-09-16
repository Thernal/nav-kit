package io.thernal.navkit.navigation.impl.domain.navigator

import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEvent
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Adapts a caller-owned back stack into the [Navigator] command surface. Holds no state of its own:
 * `NavigationHost` builds one per host, over whatever backs `NavigationHostParams.backStack`.
 *
 * `buildBackStack`/`resolveCanPop` are named apart from the [buildStack]/[canPop] members they
 * back — a same-named property and override recurse into each other instead of resolving to the
 * constructor value. [resolveGuardRunner] is a supplier for the same reason the other two are: a
 * host derives its runner from its own `NavigationHostParams.guards`, and a navigator that captured
 * one at construction would keep using it after those changed.
 *
 * **Every command goes through [mutate], and [mutate] is the only place a stack is written.** It
 * hands the stack the command started from and the stack the command proposes to the guard runner
 * together, before anything is written back. So `push`, `navigate`, `replace`, `replaceAll`, both
 * `popBack`s, `popBackTo` and a caller's own [buildStack] are guarded by construction, rather than
 * each one remembering to ask — which is what previously left `buildStack` unguarded and had
 * `replaceAll` checking only its last route.
 *
 * Pops are guarded too: a guard sees the transition, so refusing one is how a screen with unsaved
 * work says so.
 */
class BackStackNavigator(
    private val buildBackStack: (MutableList<Route>.() -> Unit) -> Unit,
    private val resolveCanPop: () -> Boolean,
    private val resolveGuardRunner: () -> NavigationGuardRunner,
    private val backDispatcher: BackDispatcher,
    private val events: NavigationEventSink = NavigationEventSink.NoOp,
) : Navigator {

    override fun buildStack(builder: MutableList<Route>.() -> Unit): NavigationOutcome {
        return mutate(
            builder = builder,
            event = { old, applied ->
                if (applied == old) {
                    null
                } else {
                    NavigationEvent.ReplaceAll(applied)
                }
            },
        )
    }

    override fun canPop(): Boolean {
        return resolveCanPop()
    }

    override fun push(route: Route): NavigationOutcome {
        return mutate(
            builder = { add(route) },
            event = { old, _ -> NavigationEvent.Push(previous = old.lastOrNull(), pushed = route) },
        )
    }

    /**
     * One stack write, not a pop followed by a push: the guards see the destination the caller
     * actually asked for, and a caller-supplied [predicate] is matched against the stack as it
     * stands rather than against a stack half-way through being rebuilt.
     */
    override fun navigate(
        route: Route,
        predicate: ((Route) -> Boolean)?,
    ): NavigationOutcome {
        val matcher = predicate ?: { candidate -> candidate == route }
        return mutate(
            builder = {
                val index = indexOfLast(matcher)
                if (index == -1) {
                    add(route)
                } else {
                    val next = take(index + 1)
                    clear()
                    addAll(next)
                }
            },
            event = { old, applied ->
                when {
                    applied == old -> null

                    applied.size > old.size ->
                        NavigationEvent.Push(previous = old.lastOrNull(), pushed = route)

                    else -> NavigationEvent.ReplaceAll(applied)
                }
            },
        )
    }

    override fun replace(route: Route): NavigationOutcome {
        return mutate(
            builder = {
                if (isNotEmpty()) {
                    removeLastOrNull()
                }
                add(route)
            },
            event = { old, _ -> NavigationEvent.Replace(old = old.lastOrNull(), new = route) },
        )
    }

    override fun replaceAll(routes: List<Route>): NavigationOutcome {
        require(routes.isNotEmpty()) { "Navigation back stack cannot be empty" }
        return mutate(
            builder = {
                clear()
                addAll(routes)
            },
            event = { _, applied -> NavigationEvent.ReplaceAll(applied) },
        )
    }

    override fun popBack(force: Boolean): Boolean {
        // The same dispatcher the host's own back gesture goes through, consulted here so a screen
        // that intercepts back is heard whichever of the two triggered it. Previously only the
        // gesture asked, and an in-app back button calling this bypassed every callback.
        if (backDispatcher.dispatch()) {
            return true
        }
        var before: ImmutableList<Route> = persistentListOf()
        var popped: Route? = null
        val outcome = mutate(
            builder = {
                before = toImmutableList()
                if (size >= 2 || force) {
                    popped = lastOrNull()
                    removeLastOrNull()
                }
            },
            event = { _, applied ->
                popped?.let { route ->
                    NavigationEvent.Pop(popped = route, backTo = applied.lastOrNull())
                }
            },
        )
        return outcome.stack != before
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
        mutate(
            builder = {
                val index = indexOfLast(predicate)
                if (index != -1) {
                    val endExclusive = if (inclusive) {
                        index
                    } else {
                        index + 1
                    }
                    val next = take(endExclusive)
                    if (next.isNotEmpty()) {
                        didPop = true
                        clear()
                        addAll(next)
                    }
                }
            },
            event = { old, applied ->
                if (applied == old) {
                    null
                } else {
                    NavigationEvent.ReplaceAll(applied)
                }
            },
        )
        return didPop
    }

    /**
     * Runs [builder] against the current stack, resolves what it produced through the host's guard
     * runner, and writes back what the guards allowed. [event] describes the command in its own
     * terms and is emitted only when the guards left the proposal intact; otherwise the refusal or
     * the deferral is what gets reported, so the event stream never claims a push that did not
     * happen.
     */
    private fun mutate(
        builder: MutableList<Route>.() -> Unit,
        event: (old: ImmutableList<Route>, applied: ImmutableList<Route>) -> NavigationEvent?,
    ): NavigationOutcome {
        var outcome: NavigationOutcome? = null
        buildBackStack {
            val old = toImmutableList()
            builder()
            val intended = toImmutableList()
            val verdict = resolveGuardRunner().resolveDeferrable(old = old, new = intended)
            val applied = when (verdict) {
                is GuardVerdict.Resolved -> verdict.stack
                is GuardVerdict.Deferred -> verdict.meanwhile
            }
            if (applied != intended) {
                clear()
                addAll(applied)
            }
            outcome = when {
                verdict is GuardVerdict.Deferred -> {
                    events.emit(NavigationEvent.Deferred(attempted = intended, meanwhile = applied))
                    NavigationOutcome.Deferred(applied)
                }

                applied == intended -> {
                    event(old, applied)?.let { emitted -> events.emit(emitted) }
                    NavigationOutcome.Applied(applied)
                }

                else -> {
                    val reason = (verdict as GuardVerdict.Resolved).reason
                    events.emit(
                        NavigationEvent.Blocked(
                            attempted = intended,
                            applied = applied,
                            reason = reason,
                        ),
                    )
                    NavigationOutcome.Rewritten(stack = applied, reason = reason)
                }
            }
        }
        return checkNotNull(outcome) { "buildBackStack did not run its builder" }
    }
}
