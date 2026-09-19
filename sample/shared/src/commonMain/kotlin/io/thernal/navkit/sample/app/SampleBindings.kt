package io.thernal.navkit.sample.app

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.domain.DeepLinkBase
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

/**
 * The two multibindings the sample itself declares. The kit declares its own — guards, deep-link
 * handlers, deep-link bases, event sinks — in `NavigationWiring`; these are the application's.
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

    companion object {
        /** Written by the root, which resolves links, and read by the screens that send them. */
        @Provides
        @SingleIn(AppScope::class)
        fun provideDeepLinkLog(): DeepLinkLog {
            return DeepLinkLog()
        }

        /**
         * The scheme the Android intent filter and the iOS URL type declare. The two lists have to
         * agree: a scheme the platform delivers but nothing registers here resolves to `NotFound`.
         */
        @Provides
        @IntoSet
        fun provideAppSchemeBase(): DeepLinkBase {
            return DeepLinkBase("navkit://")
        }

        /**
         * The web origin whose links open the same pages. The platform never delivers these to the
         * sample — it owns no domain — but the playground does, and a link on any other domain is
         * refused.
         */
        @Provides
        @IntoSet
        fun provideWebOriginBase(): DeepLinkBase {
            return DeepLinkBase("https://example.com")
        }

        /** Every navigator command, printed to the console. */
        @Provides
        @IntoSet
        fun provideLoggingSink(): NavigationEventSink {
            return NavigationEventSink { event -> println(event.message) }
        }
    }
}
