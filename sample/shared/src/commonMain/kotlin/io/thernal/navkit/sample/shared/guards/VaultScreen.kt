package io.thernal.navkit.sample.shared.guards

import androidx.compose.runtime.Composable
import io.thernal.navkit.sample.shared.ui.DemoButton
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.GUARDS.accent

@Composable
fun VaultScreen(session: PinSession) {
    SampleScreen(
        title = "Savings vault",
        topic = Topic.GUARDS,
        howItWorks = {
            Explanation(
                "Simulate a 401 from here: the guard's `meanwhile` replaces this screen with the PIN " +
                    "pad in one step, so the balance is off the screen while the PIN is asked for — " +
                    "and the same stack comes back afterwards, not a rebuilt one.",
            )
        },
    ) {
        HeroCard(
            title = "€12,480.00",
            emoji = "🔐",
            accent = accent,
            subtitle = "Savings vault · 2.1% interest",
        )
        SectionLabel(text = "Recent activity")
        ListRow(
            title = "Interest",
            emoji = "📈",
            accent = SamplePalette.Green,
            subtitle = "1 Sep",
            trailing = "+€18.20",
        )
        ListRow(
            title = "From Everyday",
            emoji = "↔️",
            accent = SamplePalette.Indigo,
            subtitle = "28 Aug",
            trailing = "+€500.00",
        )
        ListRow(title = "Round-ups", emoji = "🪙", accent = accent, subtitle = "This month", trailing = "+€3.40")
        DemoButton(label = "Simulate a 401 — session expired", onClick = { session.lock() })
    }
}
