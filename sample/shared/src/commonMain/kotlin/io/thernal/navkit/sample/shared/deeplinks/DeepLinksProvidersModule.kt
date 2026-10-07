package io.thernal.navkit.sample.shared.deeplinks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.DeepLinkLog
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface DeepLinksProvidersModule {
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
        fun provideDeepLinksGraphProvider(
            ingress: DeepLinkIngress,
            log: DeepLinkLog,
        ): NavigationGraphProvider {
            return DeepLinksGraphProvider(ingress = ingress, log = log)
        }

        @Provides
        @IntoSet
        fun provideLinkPlaygroundExample(): SampleExample {
            return SampleExample(
                topic = Topic.DEEP_LINKS,
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
                topic = Topic.DEEP_LINKS,
                kind = ExampleKind.ADVANCED,
                title = "Campaign links",
                summary = "A link that lands three screens deep, reads its source, and meets a guard.",
                route = LinkCampaignRoute,
            )
        }
    }
}
