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
 * The stack this host last rendered. A plain holder, not snapshot state: it is read while composing,
 * and a snapshot read here would subscribe the host to its own write and recompose forever. `null`
 * until the first render, which is not the same as an empty stack.
 */
private class RenderedBackStack {
    var value: ImmutableList<Route>? = null
}

/**
 * What the guards made of the stack the caller handed in. [resolved] is what may be rendered and is
 * not necessarily [NavigationHostParams.backStack]; [verdict] and [proposed] travel with it because
 * a deferral is awaited by whoever also holds the navigator, which is not this function.
 */
internal class GuardedBackStack<R : Route>(
    val proposed: ImmutableList<R>,
    val resolved: ImmutableList<R>,
    val verdict: GuardVerdict,
    val runner: NavigationGuardRunner,
)

/**
 * Resolves the caller's back stack against the guards, once per stack, and reports a rewrite back
 * through [writer] so the host recognises the correction when it comes back.
 *
 * The navigator resolves everything it writes, but it is not the only way a stack reaches a host: a
 * deep link the root applied and a stack restored after process death both land here. A pure
 * derivation, so `NavDisplay` is never handed the unresolved stack and a refused route never renders.
 */
@Composable
internal fun <R : Route> rememberGuardedBackStack(
    params: NavigationHostParams<R>,
    guardRunner: NavigationGuardRunner,
    writer: HostStackWriter<R>,
): GuardedBackStack<R> {
    val renderedBackStack = remember { RenderedBackStack() }
    val inEffect = renderedBackStack.value

    // App-wide guards plus this host's own; `extendedWith` returns the app-wide runner when it has
    // none, which is the usual case.
    val hostRunner = remember(params.guards) {
        guardRunner.extendedWith(params.guards)
    }

    // Revalidation: a guard announcing a changed answer moves nothing, but the stack may no longer
    // be one the guards allow. Collected only while mounted, so an unmounted host costs nothing.
    var generation by remember(hostRunner) { mutableIntStateOf(0) }
    LaunchedEffect(hostRunner) {
        hostRunner.invalidations.collect { generation += 1 }
    }

    // A restored stack can carry a route whose resolver did not survive the process — a guard's
    // placeholder. Dropped before anything else, so it cannot become a screen nobody can leave.
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

    // First composition: nothing is in effect, so the stack handed in is also the stack in effect —
    // "may this stand", not a transition. An empty `old` would claim a transition that never
    // happened, and a guard refusing by handing it back would return an empty stack, on the very
    // path a cold-start deep link takes. `generation` is a key because revalidation asks the same
    // question of an unmoved stack.
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
