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
import kotlinx.collections.immutable.toImmutableList

/**
 * What one call to [BackStackNavigator.mutate] did. [didMove] compares against [old] rather than
 * trusting the command's own bookkeeping: a command knows only what it *asked* for, and a guard can
 * hand the previous stack back afterwards.
 */
private class Mutation(
    val old: ImmutableList<Route>,
    val attempted: ImmutableList<Route>,
    val outcome: NavigationOutcome,
    val deferral: GuardVerdict.Deferred?,
) {
    val didMove: Boolean
        get() {
            return outcome.stack != old
        }
}

/**
 * Adapts a caller-owned back stack into the [Navigator] command surface. Holds no state of its own:
 * `NavigationHost` builds one per host, over whatever backs `NavigationHostParams.backStack`.
 *
 * **Every command goes through [mutate], and [mutate] is the only place a stack is written**, so
 * every one of them is guarded by construction — pops included, which is how a screen with unsaved
 * work refuses to be left.
 *
 * The constructor takes suppliers, not values: a runner or a stack captured at construction would
 * outlive the host's own. `buildBackStack`/`resolveCanPop` are named apart from the
 * [buildStack]/[canPop] members they back because a same-named property and override recurse.
 *
 * The navigator has no scope, so it hands every deferral it meets to [onDeferred] and the host
 * awaits it; [onMoved] tells the host a waiting deferral has been walked away from.
 */
class BackStackNavigator(
    private val buildBackStack: (MutableList<Route>.() -> Unit) -> Unit,
    private val resolveCanPop: () -> Boolean,
    private val resolveGuardRunner: () -> NavigationGuardRunner,
    private val backDispatcher: BackDispatcher,
    private val events: NavigationEventSink = NavigationEventSink.NoOp,
    private val onDeferred: (attempted: ImmutableList<Route>, deferral: GuardVerdict.Deferred) -> Unit = { _, _ -> },
    private val onMoved: () -> Unit = {},
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
        ).outcome
    }

    override fun canPop(): Boolean {
        return resolveCanPop()
    }

    override fun push(route: Route): NavigationOutcome {
        return mutate(
            builder = { add(route) },
            event = { old, _ -> NavigationEvent.Push(previous = old.lastOrNull(), pushed = route) },
        ).outcome
    }

    /**
     * One stack write, not a pop followed by a push: the guards see the destination the caller asked
     * for, and [predicate] matches the stack as it stands rather than one half-way rebuilt.
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
        ).outcome
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
        ).outcome
    }

    override fun replaceAll(routes: List<Route>): NavigationOutcome {
        require(routes.isNotEmpty()) { "Navigation back stack cannot be empty" }
        return mutate(
            builder = {
                clear()
                addAll(routes)
            },
            event = { _, applied -> NavigationEvent.ReplaceAll(applied) },
        ).outcome
    }

    override fun popBack(force: Boolean): Boolean {
        // The same dispatcher the host's back gesture goes through, so a screen that intercepts back
        // is heard whichever of the two triggered it.
        if (backDispatcher.dispatch()) {
            return true
        }
        var popped: Route? = null
        return mutate(
            builder = {
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
        ).didMove
    }

    override fun popBack(count: Int): Boolean {
        require(count >= 0) { "count must be non-negative" }
        var didChange = false
        repeat(count) { didChange = popBack() || didChange }
        return didChange
    }

    /**
     * Answers whether the stack moved, not whether a match was found — a guard refusing the jump, or
     * a predicate matching the top route, leaves it where it was.
     */
    override fun popBackTo(
        inclusive: Boolean,
        predicate: (Route) -> Boolean,
    ): Boolean {
        return mutate(
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
        ).didMove
    }

    /**
     * Runs [builder] against the current stack, resolves it through the host's guard runner, and
     * writes back what the guards allowed. [event] is emitted only when they left the proposal
     * intact, so the event stream never claims a push that did not happen.
     *
     * [onMoved] runs before [onDeferred]: a command that moves the stack walks away from the waiting
     * deferral before the one it may have started replaces it.
     */
    private fun mutate(
        builder: MutableList<Route>.() -> Unit,
        event: (old: ImmutableList<Route>, applied: ImmutableList<Route>) -> NavigationEvent?,
    ): Mutation {
        var mutation: Mutation? = null
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
            val outcome = when {
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
            mutation = Mutation(
                old = old,
                attempted = intended,
                outcome = outcome,
                deferral = verdict as? GuardVerdict.Deferred,
            )
        }
        val result = checkNotNull(mutation) { "buildBackStack did not run its builder" }
        if (result.didMove) {
            onMoved()
        }
        result.deferral?.let { deferral -> onDeferred(result.attempted, deferral) }
        return result
    }
}
