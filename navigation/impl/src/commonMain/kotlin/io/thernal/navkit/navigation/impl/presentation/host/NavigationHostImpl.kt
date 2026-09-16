package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.ui.NavDisplay
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator

/**
 * Assembles one mounted host out of three parts, each of which owns one question:
 * [rememberGuardedBackStack] what may be rendered, [rememberHostNavigator] how it is commanded,
 * and [rememberNavDisplayConfig] how it is drawn. What is left here is the wiring between them —
 * which is also the only place that holds both a verdict and a navigator, and therefore the only
 * place a deferral can be awaited.
 *
 * The `Impl` suffix is the module's rule for a declaration that would otherwise collide with an
 * `api` name: `api`'s `NavigationHost` only reads
 * [io.thernal.navkit.navigation.api.presentation.host.LocalNavigationHostRenderer] and hands the
 * call on, and this is where it lands. Calling that one from here would loop back through the
 * renderer forever, which is exactly the misreading the suffix removes.
 */
@Composable
internal fun <R : Route> NavigationHostImpl(
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

    val guarded = rememberGuardedBackStack(params = params, guardRunner = guardRunner)
    val navigator = rememberHostNavigator(
        backStack = guarded.resolved,
        onBackStackChange = params.onBackStackChange,
        guardRunner = guarded.runner,
        backDispatcher = backDispatcher,
        events = events,
    )

    // The one place a deferral is awaited. The host owns a scope tied to its own composition, so an
    // unmounted host cancels what it started, and keying the effect on the verdict itself means the
    // same deferral is never launched twice however many times this recomposes. A settled verdict
    // is applied through the navigator, which resolves it again like any other stack change; a
    // guard that defers a second time for the same stack is left alone rather than spun on.
    LaunchedEffect(guarded.verdict) {
        val verdict = guarded.verdict
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

    val config = rememberNavDisplayConfig(params = params, entries = entries)

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalBackDispatcher provides backDispatcher,
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = guarded.resolved,
            // The navigator consults the same dispatcher, so back has one path whether it came from
            // here or from a button in a screen calling `popBack()` itself.
            onBack = { navigator.popBack() },
            sceneStrategies = config.sceneStrategies,
            transitionSpec = config.transitionSpec,
            popTransitionSpec = config.popTransitionSpec,
            predictivePopTransitionSpec = config.predictivePopTransitionSpec,
            entryDecorators = config.decorators,
            entryProvider = config.entryProvider,
        )
    }
}
