package io.thernal.navkit.sample.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

/**
 * The two multibindings the sample itself declares. The kit declares its own — guards, deep-link
 * handlers, event sinks — in `NavigationWiring`; these are the application's.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface SampleBindings {
    /** Each example contributes its screens here, so the composition root imports none of them. */
    @Multibinds(allowEmpty = true)
    val graphProviders: Set<NavigationGraphProvider>

    /** …and its catalog entry here, so the index screen imports none of them either. */
    @Multibinds(allowEmpty = true)
    val examples: Set<SampleExample>
}
