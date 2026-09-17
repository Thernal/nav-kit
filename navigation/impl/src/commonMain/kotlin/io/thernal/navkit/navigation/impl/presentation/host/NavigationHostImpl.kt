package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.ui.NavDisplay
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentPruner
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator

/**
 * Assembles one mounted host out of three parts — [rememberGuardedBackStack] what may be rendered,
 * [rememberHostNavigators] how it is commanded, [rememberNavDisplayConfig] how it is drawn — and is
 * the only place holding a verdict, a navigator and a scope together, so the only place a deferral
 * can be awaited.
 */
@Composable
internal fun <R : Route> NavigationHostImpl(
    params: NavigationHostParams<R>,
    guardRunner: NavigationGuardRunner,
    backDispatcher: BackDispatcher,
    argumentPruner: ArgumentPruner,
    events: NavigationEventSink,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<R>.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        return
    }

    val currentOnBackStackChange by rememberUpdatedState(params.onBackStackChange)
    val writer = remember { HostStackWriter<R> { stack -> currentOnBackStackChange(stack) } }
    val deferrals = remember { HostDeferrals() }

    val guarded = rememberGuardedBackStack(params = params, guardRunner = guardRunner, writer = writer)
    val navigators = rememberHostNavigators(
        backStack = guarded.resolved,
        writer = writer,
        deferrals = deferrals,
        guardRunner = guarded.runner,
        backDispatcher = backDispatcher,
        events = events,
    )

    // A stack this host did not write — a deep link, a tab bar handing over a different list — is a
    // way out of a waiting deferral. After composition, so it runs before this frame submits one.
    SideEffect {
        if (writer.acknowledge(params.backStack)) {
            deferrals.abandon()
        }
    }

    // A deferral this host found itself, on a stack it was handed or revalidated. The navigator
    // submits its own; both land in the same slot.
    LaunchedEffect(guarded.verdict) {
        val verdict = guarded.verdict
        if (verdict is GuardVerdict.Deferred) {
            deferrals.submit(attempted = guarded.proposed, deferral = verdict)
        }
    }

    // The one place a deferral is awaited: the scope is tied to this composition, so an unmounted
    // host cancels what it started.
    val pending = deferrals.pending
    LaunchedEffect(pending) {
        if (pending != null) {
            deferrals.drive(run = pending, navigator = navigators.deferral)
        }
    }

    // An argument's lifetime is a fact about the stack, so the host that owns the stack applies it.
    // Depth 0 only — see LocalNavigationHostDepth.
    val hostDepth = LocalNavigationHostDepth.current
    LaunchedEffect(key1 = guarded.resolved, key2 = hostDepth, key3 = argumentPruner) {
        if (hostDepth == 0) {
            argumentPruner.pruneFor(guarded.resolved)
        }
    }

    val config = rememberNavDisplayConfig(params = params, entries = entries)

    CompositionLocalProvider(
        LocalNavigator provides navigators.screens,
        LocalBackDispatcher provides backDispatcher,
        LocalNavigationHostDepth provides hostDepth + 1,
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = guarded.resolved,
            // The navigator consults the same dispatcher, so back has one path whether it came from
            // here or from a screen calling `popBack()`.
            onBack = { navigators.screens.popBack() },
            sceneStrategies = config.sceneStrategies,
            transitionSpec = config.transitionSpec,
            popTransitionSpec = config.popTransitionSpec,
            predictivePopTransitionSpec = config.predictivePopTransitionSpec,
            entryDecorators = config.decorators,
            entryProvider = config.entryProvider,
        )
    }
}
