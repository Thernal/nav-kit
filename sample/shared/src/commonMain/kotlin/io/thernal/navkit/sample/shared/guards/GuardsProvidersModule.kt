package io.thernal.navkit.sample.shared.guards

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface GuardsProvidersModule {
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
        fun provideGuardsGraphProvider(
            session: SessionStore,
            pin: PinSession,
        ): NavigationGraphProvider {
            return GuardsGraphProvider(session = session, pin = pin)
        }

        @Provides
        @IntoSet
        fun provideMembersExample(): SampleExample {
            return SampleExample(
                topic = Topic.GUARDS,
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
                topic = Topic.GUARDS,
                kind = ExampleKind.ADVANCED,
                title = "401 and a PIN",
                summary = "A guard that defers: it asks for a PIN, then continues where you were going.",
                route = VaultLobbyRoute,
            )
        }
    }
}
