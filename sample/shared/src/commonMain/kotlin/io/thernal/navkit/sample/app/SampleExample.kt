package io.thernal.navkit.sample.app

import io.thernal.navkit.navigation.api.presentation.model.Route

/** Whether an example is the smallest thing that works, or the shape a real screen would have. */
enum class ExampleKind(val label: String) {
    SIMPLE("simple"),
    REAL_LIFE("real life"),
}

/**
 * One entry on the catalog screen.
 *
 * Examples register themselves into the graph, so the catalog imports none of them and adding an
 * example is adding a file — the same multibinding the kit uses for guards, deep-link handlers and
 * event sinks.
 */
data class SampleExample(
    val group: String,
    val kind: ExampleKind,
    val title: String,
    val summary: String,
    val route: Route,
)
