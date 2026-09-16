package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentPruner
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.host.NavigationHostRenderer
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * The single application-graph object this module contributes to composition. Everything per-host
 * — the navigator, the back stack adapter — is built inside [NavigationHostImpl] instead, because a
 * host can be mounted more than once.
 */
class NavigationHostRendererImpl(
    private val guardRunner: NavigationGuardRunner,
    private val backDispatcher: BackDispatcher,
    private val argumentPruner: ArgumentPruner,
    private val events: NavigationEventSink,
) : NavigationHostRenderer {
    @Composable
    override fun <R : Route> Render(
        params: NavigationHostParams<R>,
        modifier: Modifier,
        entries: EntryProviderScope<R>.() -> Unit,
    ) {
        NavigationHostImpl(
            params = params,
            guardRunner = guardRunner,
            backDispatcher = backDispatcher,
            argumentPruner = argumentPruner,
            events = events,
            modifier = modifier,
            entries = entries,
        )
    }
}
