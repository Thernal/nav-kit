package io.thernal.navkit.navigation.wiring

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.ContributesTo
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentPruner
import io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkEvents
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.impl.data.RuntimeDeepLinkBridge
import io.thernal.navkit.navigation.impl.domain.argument.NavigationArgumentsImpl

/** Binds the instances [NavigationProvidersModule] provides to the interfaces the app and the host use. */
@BindingContainer
@ContributesTo(AppScope::class)
interface NavigationBindingsModule {
    /**
     * One instance behind two interfaces — [NavigationArguments] for the application,
     * [ArgumentPruner] for the host — so `pruneFor` stays off the composition local. The scope sits
     * on the instance these alias; scoping the aliases would build two stores, and the host would
     * prune the one nothing writes to.
     */
    @Binds
    val NavigationArgumentsImpl.bindNavigationArguments: NavigationArguments

    @Binds
    val NavigationArgumentsImpl.bindArgumentPruner: ArgumentPruner

    /**
     * The same aliasing: the publisher an entry point calls and the stream the root collects have
     * to be one object, or a cold-start link is dropped on the floor.
     */
    @Binds
    val RuntimeDeepLinkBridge.bindDeepLinkIngress: DeepLinkIngress

    @Binds
    val RuntimeDeepLinkBridge.bindDeepLinkEvents: DeepLinkEvents
}
