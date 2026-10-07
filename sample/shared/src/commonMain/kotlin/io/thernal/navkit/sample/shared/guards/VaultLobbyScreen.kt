package io.thernal.navkit.sample.shared.guards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.DemoButton
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.GUARDS.accent

@Composable
fun VaultLobbyScreen(session: PinSession) {
    val navigator = LocalNavigator.current
    val isLocked by session.locked.collectAsState()
    val vaultState = if (isLocked) {
        "🔒" to "Session expired — PIN required"
    } else {
        "🔐" to "PIN protected"
    }

    SampleScreen(
        title = "Accounts",
        topic = Topic.GUARDS,
        howItWorks = {
            LiveValue(label = "Session locked", value = isLocked.toString())
            Explanation(
                "A 401 arrives mid-session: the destination is still right, the session is not. " +
                    "Simulate one, then open the vault — the guard *defers* instead of redirecting, " +
                    "shows the PIN pad meanwhile, and continues to the vault once the PIN is right. " +
                    "Get it wrong and the vault is refused instead.",
            )
        },
    ) {
        HeroCard(
            title = "€14,302.55",
            emoji = "🏦",
            accent = SamplePalette.Indigo,
            subtitle = "Total balance across 2 accounts",
        )
        SectionLabel(text = "Accounts")
        ListRow(
            title = "Everyday",
            emoji = "💳",
            accent = SamplePalette.Indigo,
            subtitle = "•••• 4242",
            trailing = "€1,822.55",
        )
        ListRow(
            title = "Savings vault",
            emoji = vaultState.first,
            accent = accent,
            subtitle = vaultState.second,
            trailing = "€12,480.00",
            onClick = { navigator.push(VaultRoute) },
        )
        DemoButton(label = "Simulate a 401 — session expired", onClick = { session.lock() })
    }
}
