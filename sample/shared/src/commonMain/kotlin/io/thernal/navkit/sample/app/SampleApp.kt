package io.thernal.navkit.sample.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams

/**
 * The composition root.
 *
 * Three things happen, and they are the three things every application using this kit does. The
 * graph's composition locals are installed in one spread, so no screen ever imports the navigation
 * module to reach a navigator, a results mailbox or an argument store. The root back stack is owned
 * outside the host, by [RootViewModel]. And every feature's screens are registered from the
 * `NavigationGraphProvider` set, so this file names none of them.
 *
 * [graph] is handed in rather than created here: it lives as long as the process — the Android
 * `Application`, the iOS entry point — and a composition does not. A graph remembered by the
 * composition was rebuilt on every rotation.
 */
@Composable
fun SampleApp(graph: SampleGraph) {
    // `values =` rather than a spread: the root installs whatever the graph collected without
    // naming a single one of them, and the named vararg says so without a suppression.
    CompositionLocalProvider(
        values = graph.providedValues.toTypedArray(),
    ) {
        MaterialTheme {
            val root: RootViewModel = viewModel { RootViewModel() }
            val backStack by root.backStack.collectAsState()

            // Resolving an inbound link is the root state holder's job: a host can be mounted
            // anywhere, but there is only one link stream for the whole application.
            LaunchedEffect(graph) {
                graph.deepLinkEvents.links.collect { incoming ->
                    val outcome = graph.deepLinkDispatcher.dispatch(
                        raw = incoming.uri,
                        source = incoming.source,
                    )
                    if (outcome is DeepLinkOutcome.Navigate) {
                        root.onDeepLink(outcome.routes)
                    }
                }
            }

            NavigationHost(
                params = NavigationHostParams(
                    backStack = backStack,
                    onBackStackChange = root::onBackStackChange,
                ),
            ) {
                for (provider in graph.graphProviders) {
                    with(provider) { provide() }
                }
            }
        }
    }
}
