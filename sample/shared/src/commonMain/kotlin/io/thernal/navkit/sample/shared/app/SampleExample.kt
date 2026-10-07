package io.thernal.navkit.sample.shared.app

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.sample.shared.ui.Topic

/**
 * One entry on the catalog screen.
 *
 * Examples register themselves into the graph, so the catalog imports none of them and adding an
 * example is adding a file — the same multibinding the kit uses for guards, deep-link handlers and
 * event sinks.
 */
data class SampleExample(
    val topic: Topic,
    val kind: ExampleKind,
    val title: String,
    val summary: String,
    val route: Route,
)
