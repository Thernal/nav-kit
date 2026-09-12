package io.thernal.navkit.navigation.impl.domain.guard

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * Bounds the fixpoint below. Two guards that rewrite each other's output would otherwise spin
 * forever; reaching this many rounds is a bug in the guards, not a stack worth returning.
 */
private const val MAX_ROUNDS = 8

/**
 * Folds every guard over the proposed stack, then repeats the fold until the stack stops changing.
 *
 * The repetition is what closes the hole a per-route runner has: a redirect target used to be
 * pushed exactly as the guard named it, unseen by every guard including the one that produced it.
 * Here a rewritten stack is simply the next proposal, so an injected route is guarded like any
 * other.
 *
 * Each guard's output is checked before it is adopted. A guard may drop routes and insert routes,
 * but reordering what it keeps or emptying the stack corrupts navigation state for every other
 * feature, so it fails loudly rather than being silently repaired — the same choice
 * `DeepLinkDispatcherImpl` makes for two handlers claiming one page.
 */
class NavigationGuardRunnerImpl(private val guards: List<NavigationGuard>) : NavigationGuardRunner {

    // Built once rather than per collector: the merge is over a fixed list, and a host resubscribes
    // whenever its runner changes identity.
    override val invalidations: Flow<Unit> = guards.map { guard -> guard.invalidations }.merge()

    override fun extendedWith(guards: ImmutableList<NavigationGuard>): NavigationGuardRunner {
        if (guards.isEmpty()) {
            return this
        }
        return NavigationGuardRunnerImpl(this.guards + guards)
    }

    override fun resolve(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict.Resolved {
        return when (val verdict = resolveDeferrable(old = old, new = new)) {
            is GuardVerdict.Resolved -> verdict
            is GuardVerdict.Deferred -> GuardVerdict.Resolved(stack = verdict.meanwhile)
        }
    }

    override fun resolveDeferrable(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict {
        if (guards.isEmpty()) {
            return GuardVerdict.Resolved(new)
        }
        var current = new
        var reason: BlockReason? = null
        var round = 0
        while (round < MAX_ROUNDS) {
            // A round settles only when no guard rewrote anything, not when the round happens to
            // end where it started: two guards that undo each other leave a stack the first one
            // would reject again, and comparing only the round's net effect calls that settled.
            var didRewrite = false
            guards.forEach { guard ->
                when (val verdict = guard.evaluate(old = old, new = current)) {
                    is GuardVerdict.Deferred -> {
                        guard.requireWellFormed(proposed = current, candidate = verdict.meanwhile)
                        return verdict
                    }

                    is GuardVerdict.Resolved -> {
                        if (verdict.stack != current) {
                            guard.requireWellFormed(proposed = current, candidate = verdict.stack)
                            reason = verdict.reason ?: reason
                            current = verdict.stack
                            didRewrite = true
                        }
                    }
                }
            }
            if (!didRewrite) {
                return GuardVerdict.Resolved(stack = current, reason = reason)
            }
            round++
        }
        error(
            "Navigation guards did not settle after $MAX_ROUNDS rounds; two of " +
                "${guards.joinToString { guard -> guard.typeName() }} rewrite each other's stack.",
        )
    }
}

private fun NavigationGuard.requireWellFormed(
    proposed: ImmutableList<Route>,
    candidate: ImmutableList<Route>,
) {
    if (candidate.isEmpty() && proposed.isNotEmpty()) {
        error("${typeName()} emptied the back stack; a guard may refuse a stack, never leave none.")
    }
    val keptInProposed = proposed.filter { route -> route in candidate }
    val keptInCandidate = candidate.filter { route -> route in proposed }
    if (keptInProposed != keptInCandidate) {
        error("${typeName()} reordered the routes it kept; a guard may drop and insert, not reorder.")
    }
    val hasNewDuplicate = candidate.size != candidate.distinct().size &&
        proposed.size == proposed.distinct().size
    if (hasNewDuplicate) {
        error("${typeName()} returned a stack with a duplicate route: $candidate")
    }
}

private fun Any.typeName(): String {
    return this::class.simpleName ?: toString()
}
