package io.thernal.navkit.sample.deeplinks

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.DeepLinkLog
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample

private class DeepLinksGraph(
    private val ingress: DeepLinkIngress,
    private val log: DeepLinkLog,
) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<LinkPlaygroundRoute> { LinkPlaygroundScreen(ingress = ingress, log = log) }
        navEntry<ProductRoute> { route -> ProductScreen(route) }
        navEntry<LinkCampaignRoute> { LinkCampaignScreen(ingress = ingress, log = log) }
        navEntry<OrdersRoute> { OrdersScreen() }
        navEntry<OrderRoute> { route -> OrderScreen(route) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface DeepLinksBindings {
    companion object {
        @Provides
        @IntoSet
        fun provideProductHandler(): DeepLinkHandler {
            return ProductDeepLinkHandler()
        }

        @Provides
        @IntoSet
        fun provideOrdersHandler(): DeepLinkHandler {
            return OrdersDeepLinkHandler()
        }

        @Provides
        @IntoSet
        fun provideSecretHandler(): DeepLinkHandler {
            return SecretDeepLinkHandler()
        }

        @Provides
        @IntoSet
        fun provideDeepLinksGraph(
            ingress: DeepLinkIngress,
            log: DeepLinkLog,
        ): NavigationGraphProvider {
            return DeepLinksGraph(ingress = ingress, log = log)
        }

        @Provides
        @IntoSet
        fun provideLinkPlaygroundExample(): SampleExample {
            return SampleExample(
                group = "Deep links",
                kind = ExampleKind.SIMPLE,
                title = "One link, one route",
                summary = "Deliver a URI the way the platform would; the app scheme and the web form agree.",
                route = LinkPlaygroundRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideCampaignExample(): SampleExample {
            return SampleExample(
                group = "Deep links",
                kind = ExampleKind.REAL_LIFE,
                title = "Campaign links",
                summary = "A link that lands three screens deep, reads its source, and meets a guard.",
                route = LinkCampaignRoute,
            )
        }
    }
}
