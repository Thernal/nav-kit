package io.thernal.navkit.sample.guards

import androidx.navigation3.runtime.EntryProviderScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.app.ExampleKind
import io.thernal.navkit.sample.app.SampleExample

private class GuardsGraph(
    private val session: SessionStore,
    private val pin: PinSession,
) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<MembersHomeRoute> { MembersHomeScreen(session) }
        navEntry<MembersSecretRoute> { MembersSecretScreen(session) }
        navEntry<SignInRoute> { route -> SignInScreen(route = route, session = session) }
        navEntry<VaultLobbyRoute> { VaultLobbyScreen(pin) }
        navEntry<VaultRoute> { VaultScreen(pin) }
        navEntry<PinEntryRoute> { PinEntryScreen(pin) }
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
interface GuardsBindings {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideSessionStore(): SessionStore {
            return SessionStore()
        }

        @Provides
        @SingleIn(AppScope::class)
        fun providePinSession(): PinSession {
            return PinSession()
        }

        @Provides
        @IntoSet
        fun provideAuthGuard(session: SessionStore): NavigationGuard {
            return AuthGuard(session)
        }

        @Provides
        @IntoSet
        fun providePinGuard(session: PinSession): NavigationGuard {
            return PinGuard(session)
        }

        @Provides
        @IntoSet
        fun provideGuardsGraph(
            session: SessionStore,
            pin: PinSession,
        ): NavigationGraphProvider {
            return GuardsGraph(session = session, pin = pin)
        }

        @Provides
        @IntoSet
        fun provideMembersExample(): SampleExample {
            return SampleExample(
                group = "Guards",
                kind = ExampleKind.SIMPLE,
                title = "Members area",
                summary = "A destination rule that also removes the page when the session ends.",
                route = MembersHomeRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideVaultExample(): SampleExample {
            return SampleExample(
                group = "Guards",
                kind = ExampleKind.REAL_LIFE,
                title = "401 and a PIN",
                summary = "A guard that defers: it asks for a PIN, then continues where you were going.",
                route = VaultLobbyRoute,
            )
        }
    }
}
