package io.thernal.navkit.navigation.wiring

import androidx.compose.runtime.ProvidedValue
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkDispatcher
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkEvents
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.host.LocalNavigationHostRenderer
import io.thernal.navkit.navigation.api.presentation.host.NavigationHostRenderer
import io.thernal.navkit.navigation.api.presentation.log.NavigationEvent
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.NavigationResults
import io.thernal.navkit.navigation.impl.data.RuntimeDeepLinkBridge
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.deeplink.DeepLinkDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.result.NavigationResultsImpl
import io.thernal.navkit.navigation.impl.presentation.host.NavigationHostRendererImpl

/**
 * Binds the navigation capability into an application graph.
 *
 * This module is the worked example of how the other two are installed, not a fixed part of them:
 * `api` and `impl` name no injection framework at all, so an app on a different container writes
 * its own equivalent of this file and changes nothing else. Metro is what this repository happens
 * to demonstrate it with.
 *
 * `Navigator` is deliberately **not** bound here. It holds no state — `NavigationHost` builds one
 * per host over the caller-owned `backStack`/`onBackStackChange` pair on its `NavigationHostParams`
 * — so an application-graph binding for it would be a second, unscoped navigator pointing at
 * nothing.
 */
@BindingContainer
@ContributesTo(AppScope::class)
interface NavigationWiring {
    /** Features contribute `@IntoSet`; an app with none still resolves the empty set. */
    @Multibinds(allowEmpty = true)
    val deepLinkHandlers: Set<DeepLinkHandler>

    /** Features contribute `@IntoSet`; an app with none still resolves the empty set. */
    @Multibinds(allowEmpty = true)
    val navigationGuards: Set<NavigationGuard>

    /**
     * Observers of [NavigationEvent]. A multibinding rather than a single overridable binding: a
     * debug console, an analytics tracker and a test recorder all want the same stream, and none
     * of them should have to displace the others to get it.
     */
    @Multibinds(allowEmpty = true)
    val navigationEventSinks: Set<NavigationEventSink>

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideBackDispatcher(): BackDispatcher {
            return BackDispatcherImpl()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideNavigationResults(): NavigationResults {
            return NavigationResultsImpl()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideNavigationGuardRunner(guards: Set<NavigationGuard>): NavigationGuardRunner {
            return NavigationGuardRunnerImpl(guards.toList())
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideDeepLinkDispatcher(handlers: Set<DeepLinkHandler>): DeepLinkDispatcher {
            return DeepLinkDispatcherImpl(handlers)
        }

        /**
         * One bridge, two contracts: the publisher an entry point calls and the stream the root
         * collects have to be the same object or a cold-start link is dropped on the floor.
         */
        @Provides
        @SingleIn(AppScope::class)
        fun provideRuntimeDeepLinkBridge(): RuntimeDeepLinkBridge {
            return RuntimeDeepLinkBridge()
        }

        @Provides
        fun provideDeepLinkIngress(bridge: RuntimeDeepLinkBridge): DeepLinkIngress {
            return bridge
        }

        @Provides
        fun provideDeepLinkEvents(bridge: RuntimeDeepLinkBridge): DeepLinkEvents {
            return bridge
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideNavigationEventSink(sinks: Set<NavigationEventSink>): NavigationEventSink {
            if (sinks.isEmpty()) {
                return NavigationEventSink.NoOp
            }
            return NavigationEventSink { event -> sinks.forEach { sink -> sink.emit(event) } }
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideNavigationHostRenderer(
            guardRunner: NavigationGuardRunner,
            backDispatcher: BackDispatcher,
            events: NavigationEventSink,
        ): NavigationHostRenderer {
            return NavigationHostRendererImpl(
                guardRunner = guardRunner,
                backDispatcher = backDispatcher,
                events = events,
            )
        }

        /**
         * Contributed into the graph's `Set<ProvidedValue<*>>` so a composition root installs every
         * feature's composition locals in one `CompositionLocalProvider(*values.toTypedArray())`,
         * instead of importing this module to name [LocalNavigationHostRenderer] itself.
         */
        @Provides
        @IntoSet
        fun provideNavigationHostRendererValue(renderer: NavigationHostRenderer): ProvidedValue<*> {
            return LocalNavigationHostRenderer provides renderer
        }

        @Provides
        @IntoSet
        fun provideNavigationResultsValue(results: NavigationResults): ProvidedValue<*> {
            return LocalNavigationResults provides results
        }
    }
}
