package io.thernal.navkit.sample.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.navigation.api.presentation.host.NavigationHost
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SampleTheme
import io.thernal.navkit.sample.ui.Topic

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
        SampleTheme {
            val root: RootViewModel = viewModel { RootViewModel() }
            val backStack by root.backStack.collectAsState()

            // Resolving an inbound link is the root state holder's job: a host can be mounted
            // anywhere, but there is only one link stream for the whole application. What the
            // handler decided is logged whether or not it navigates, so a refusal is visible.
            LaunchedEffect(graph) {
                graph.deepLinkEvents.links.collect { incoming ->
                    val outcome = graph.deepLinkDispatcher.dispatch(
                        raw = incoming.uri,
                        source = incoming.source,
                    )
                    graph.deepLinkLog.record(link = incoming, outcome = outcome)
                    if (outcome is DeepLinkOutcome.Navigate) {
                        root.onDeepLink(outcome.routes)
                    }
                }
            }

            NavigationHost(
                params = NavigationHostParams(
                    backStack = backStack,
                    onBackStackChange = root::onBackStackChange,
                    fallback = ::unknownRouteEntry,
                ),
            ) {
                for (provider in graph.graphProviders) {
                    with(provider) { provide() }
                }
            }
        }
    }
}

/** A reference, not a lambda, so [NavigationHostParams] stays equal across recompositions. */
private fun unknownRouteEntry(route: Route): NavEntry<Route> {
    return NavEntry(key = route) { unknown -> UnknownRouteScreen(route = unknown) }
}

@Composable
private fun UnknownRouteScreen(route: Route) {
    SampleScreen(
        title = "Not found",
        topic = Topic.NAVIGATION,
        howItWorks = {
            LiveValue(label = "Route", value = route.toString())
            Explanation(
                "The route reached the host; the host has no entry for it. Without a `fallback` this " +
                    "would be an `IllegalStateException` from the entry table. The usual causes are a " +
                    "guard substituting a route into a host that never registers it, and a deep link " +
                    "resolving to one the root does not know.",
            )
        },
    ) {
        HeroCard(
            title = "This page doesn't exist",
            emoji = "🧭",
            accent = Topic.NAVIGATION.accent,
            subtitle = "The link or screen you followed isn't part of this version of the app.",
        )
    }
}
