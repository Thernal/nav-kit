package io.thernal.navkit.sample.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams

/**
 * The composition root, and the only place the sample differs from a real application: an app would
 * hold its graph in a platform container rather than remembering it here.
 *
 * Three things happen, and they are the three things every application using this kit does. The
 * graph's composition locals are installed in one spread, so no screen ever imports the navigation
 * module to reach a navigator, a results mailbox or an argument store. The root back stack is owned
 * outside the host, by [RootViewModel]. And every feature's screens are registered from the
 * `NavigationGraphProvider` set, so this file names none of them.
 */
@Composable
fun SampleApp() {
    val graph = remember { createSampleGraph() }

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
