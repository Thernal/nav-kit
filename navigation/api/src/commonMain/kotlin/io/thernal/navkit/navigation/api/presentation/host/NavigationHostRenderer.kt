package io.thernal.navkit.navigation.api.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Renders a feature-owned back stack as the platform navigation surface. `impl` owns the
 * `NavDisplay` hierarchy, the navigator it builds per host, the overlay scene strategies and the
 * animations; `api` only ever exposes the [NavigationHost] composable below.
 */
interface NavigationHostRenderer {
    @Composable
    fun <R : Route> Render(
        params: NavigationHostParams<R>,
        modifier: Modifier,
        entries: EntryProviderScope<R>.() -> Unit,
    )
}

val LocalNavigationHostRenderer =
    compositionLocalOf<NavigationHostRenderer> { PreviewNavigationHostRenderer }

/** Draws nothing, so an isolated preview composes without the navigation feature installed. */
private object PreviewNavigationHostRenderer : NavigationHostRenderer {
    @Composable
    override fun <R : Route> Render(
        params: NavigationHostParams<R>,
        modifier: Modifier,
        entries: EntryProviderScope<R>.() -> Unit,
    ) {
        // Intentionally empty: a preview renders the screen, not the host around it.
    }
}

/**
 * Mounts a back stack. A host can sit anywhere a feature needs its own local stack (a bottom
 * sheet's internal steps, a wizard), not just at the app root — which is why it carries no
 * deep-link concept of its own: there is only one cold-start link stream for the whole app, so
 * resolving one stays the root's job.
 */
@Composable
fun <R : Route> NavigationHost(
    params: NavigationHostParams<R>,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<R>.() -> Unit,
) {
    LocalNavigationHostRenderer.current.Render(
        params = params,
        modifier = modifier,
        entries = entries,
    )
}
