package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.model.TransientRoute
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import io.thernal.navkit.navigation.impl.presentation.scene.BottomSheetSceneStrategy
import io.thernal.navkit.navigation.impl.presentation.scene.ModalSceneStrategy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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

@Composable
internal fun <R : Route> NavigationView(
    params: NavigationHostParams<R>,
    guardRunner: NavigationGuardRunner,
    backDispatcher: BackDispatcher,
    events: NavigationEventSink,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<R>.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        return
    }
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

    // The navigator resolves everything it writes, but it is not the only way a stack reaches this
    // host: the composition root applies a resolved deep link through `onBackStackChange`, and
    // process death restores one from saved state. Both land here, so the host resolves what it was
    // handed before rendering it. A pure derivation, which is what keeps a refused route off the
    // screen entirely — `NavDisplay` is never given the unresolved stack, and the correction is
    // written back to the caller one frame later.
    //
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

    @Suppress("UNCHECKED_CAST")
    val resolvedBackStack = when (verdict) {
        is GuardVerdict.Resolved -> verdict.stack as ImmutableList<R>
        is GuardVerdict.Deferred -> verdict.meanwhile as ImmutableList<R>
    }
    SideEffect {
        renderedBackStack.value = resolvedBackStack
    }

    val currentBackStack by rememberUpdatedState(resolvedBackStack)
    val currentOnBackStackChange by rememberUpdatedState(params.onBackStackChange)
    val currentHostRunner by rememberUpdatedState(hostRunner)

    LaunchedEffect(resolvedBackStack) {
        if (resolvedBackStack != params.backStack) {
            currentOnBackStackChange(resolvedBackStack)
        }
    }

    // Built once and remembered so the navigator identity stays stable across recompositions —
    // every read/write goes through the `rememberUpdatedState` closures above, never captured here.
    val navigator = remember {
        BackStackNavigator(
            buildBackStack = { builder ->
                @Suppress("UNCHECKED_CAST")
                val mutable = currentBackStack.toMutableList() as MutableList<Route>
                mutable.builder()
                @Suppress("UNCHECKED_CAST")
                currentOnBackStackChange(mutable.toImmutableList() as ImmutableList<R>)
            },
            resolveCanPop = { currentBackStack.size > 1 },
            resolveGuardRunner = { currentHostRunner },
            backDispatcher = backDispatcher,
            events = events,
        )
    }

    // The one place a deferral is awaited. The host owns a scope tied to its own composition, so an
    // unmounted host cancels what it started, and keying the effect on the verdict itself means the
    // same deferral is never launched twice however many times this recomposes. A settled verdict
    // is applied through the navigator, which resolves it again like any other stack change; a
    // guard that defers a second time for the same stack is left alone rather than spun on.
    LaunchedEffect(verdict) {
        if (verdict is GuardVerdict.Deferred) {
            val settled = verdict.resolve(navigator)
            if (settled is GuardVerdict.Resolved) {
                navigator.buildStack {
                    clear()
                    addAll(settled.stack)
                }
            }
        }
    }

    val provider = entryProvider(builder = entries)
    val pushSpec = remember(params.transitionSpec) {
        params.transitionSpec ?: NavAnimations.push()
    }
    val popSpec = remember(params.popTransitionSpec) {
        params.popTransitionSpec ?: NavAnimations.pop()
    }
    val predictiveSpec = remember(params.predictivePopTransitionSpec) {
        params.predictivePopTransitionSpec ?: NavAnimations.predictivePop()
    }
    val builtInDecorators = persistentListOf(
        rememberSaveableStateHolderNavEntryDecorator<R>(),
        rememberViewModelStoreNavEntryDecorator<R>(),
    )
    // Caller strategies come first so an app can claim a route before the built-in overlays see it;
    // SinglePaneSceneStrategy is last because it claims everything.
    val builtInScenes = remember {
        persistentListOf<SceneStrategy<R>>(BottomSheetSceneStrategy(), ModalSceneStrategy())
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalBackDispatcher provides backDispatcher,
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = resolvedBackStack,
            // The navigator consults the same dispatcher, so back has one path whether it came from
            // here or from a button in a screen calling `popBack()` itself.
            onBack = { navigator.popBack() },
            sceneStrategies = params.sceneStrategies + builtInScenes + SinglePaneSceneStrategy(),
            transitionSpec = pushSpec,
            popTransitionSpec = popSpec,
            predictivePopTransitionSpec = predictiveSpec,
            entryDecorators = builtInDecorators + params.decorators,
            entryProvider = provider,
        )
    }
}
