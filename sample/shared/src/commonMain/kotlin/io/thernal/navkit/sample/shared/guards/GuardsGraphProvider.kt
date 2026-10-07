package io.thernal.navkit.sample.shared.guards

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

internal class GuardsGraphProvider(
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
