package io.thernal.navkit.sample.app

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.createGraph
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkDispatcher
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkEvents
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

/**
 * The application graph, and the first real one in this repository — `navigation/wiring` is the
 * worked example of how the kit is installed, and this is a thing that actually installs it.
 *
 * Nothing here names a feature. Composition locals arrive in one set, screens arrive in another,
 * and the deep-link trio is asked for by contract, so adding an example never changes this file.
 */
@DependencyGraph(AppScope::class)
interface SampleGraph {
    val providedValues: Set<ProvidedValue<*>>

    val graphProviders: Set<NavigationGraphProvider>

    val examples: Set<SampleExample>

    val deepLinkEvents: DeepLinkEvents

    val deepLinkDispatcher: DeepLinkDispatcher

    val deepLinkIngress: DeepLinkIngress

    val deepLinkLog: DeepLinkLog
}

fun createSampleGraph(): SampleGraph {
    return createGraph<SampleGraph>()
}
