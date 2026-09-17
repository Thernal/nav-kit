package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.TransientRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * The stack this host last rendered, kept in a plain holder rather than in snapshot state on
 * purpose: it is read while composing, and a snapshot read here would subscribe the host to its own
 * write and recompose forever. `null` until the first render, which is not the same as an empty
 * stack — see where it is read.
 */
private class RenderedBackStack {
    var value: ImmutableList<Route>? = null
}

/**
 * What the guards made of the stack the caller handed in, and the runner that decided it.
 *
 * [resolved] is what may be rendered; it is not necessarily [NavigationHostParams.backStack], and
 * the difference is the whole reason this type exists. [verdict] and [proposed] — the stack the
 * verdict was asked about — are kept alongside it because a deferral has to be awaited by whoever
 * also holds the navigator, which is not this function.
 */
internal class GuardedBackStack<R : Route>(
    val proposed: ImmutableList<R>,
    val resolved: ImmutableList<R>,
    val verdict: GuardVerdict,
    val runner: NavigationGuardRunner,
)

/**
 * Resolves the caller's back stack against the guards, once per stack, and reports a rewrite back
 * to the caller.
 *
 * The navigator resolves everything it writes, but it is not the only way a stack reaches a host:
 * the composition root applies a resolved deep link through
 * [NavigationHostParams.onBackStackChange], and process death restores one from saved state. Both
 * land here, so the host resolves what it was handed before rendering it. A pure derivation, which
 * is what keeps a refused route off the screen entirely — `NavDisplay` is never given the
 * unresolved stack, and the correction is written back to the caller one frame later — through
 * [writer], so the host recognises it when it comes back.
 */
@Composable
internal fun <R : Route> rememberGuardedBackStack(
    params: NavigationHostParams<R>,
    guardRunner: NavigationGuardRunner,
    writer: HostStackWriter<R>,
): GuardedBackStack<R> {
    val renderedBackStack = remember { RenderedBackStack() }
    val inEffect = renderedBackStack.value

    // The application-wide guards plus whatever this particular host contributes. `extendedWith`
    // hands back the app-wide runner unchanged when there are none, which is the usual case.
    val hostRunner = remember(params.guards) {
        guardRunner.extendedWith(params.guards)
    }

    // Revalidation. A guard announcing that its answer may have changed is not a navigation, so
    // nothing about the stack moves — but the stack may no longer be one the guards allow, and
    // without this a route that has *become* invalid sits there until something else navigates.
    // Collected only while this host is composed, so an unmounted host costs nothing.
    var generation by remember(hostRunner) { mutableIntStateOf(0) }
    LaunchedEffect(hostRunner) {
        hostRunner.invalidations.collect { generation += 1 }
    }

    // A restored stack can carry routes that outlived the thing that was supposed to resolve them —
    // a guard's placeholder, whose coroutine did not survive the process. Dropping them before
    // anything else happens is what keeps that from being a screen nobody can leave.
    val proposedBackStack = remember(params.backStack) {
        if (inEffect != null) {
            params.backStack
        } else {
            val restored = params.backStack.filterNot { route -> route is TransientRoute }
            if (restored.isEmpty()) {
                params.backStack
            } else {
                restored.toImmutableList()
            }
        }
    }

    // On the first composition nothing is in effect yet, so the stack handed in is also the stack
    // in effect: no transition to reason about, only "may this stand". An empty `old` would claim a
    // transition that never happened, and a guard refusing by handing `old` back would then return
    // an empty stack — on the very path a cold-start deep link takes. A revalidation asks that same
    // question of an unmoved stack, which is why `generation` is a key here and not a second call.
    val verdict = remember(key1 = hostRunner, key2 = proposedBackStack, key3 = generation) {
        hostRunner.resolveDeferrable(
            old = inEffect ?: proposedBackStack,
            new = proposedBackStack,
        )
    }

    val resolvedBackStack: ImmutableList<R> = when (verdict) {
        is GuardVerdict.Resolved -> verdict.stack.asHostStack()
        is GuardVerdict.Deferred -> verdict.meanwhile.asHostStack()
    }
    SideEffect {
        renderedBackStack.value = resolvedBackStack
    }

    LaunchedEffect(resolvedBackStack) {
        if (resolvedBackStack != params.backStack) {
            writer.write(resolvedBackStack)
        }
    }

    return remember(proposedBackStack, resolvedBackStack, verdict, hostRunner) {
        GuardedBackStack(
            proposed = proposedBackStack,
            resolved = resolvedBackStack,
            verdict = verdict,
            runner = hostRunner,
        )
    }
}
