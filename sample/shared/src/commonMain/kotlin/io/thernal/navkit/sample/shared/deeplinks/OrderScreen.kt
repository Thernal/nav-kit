package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.DEEP_LINKS.accent

@Composable
fun OrderScreen(route: OrderRoute) {
    SampleScreen(
        title = "Order #${route.id}",
        topic = Topic.DEEP_LINKS,
        howItWorks = {
            Explanation(
                "Reached by a link or by a push — the screen cannot tell, and should not. A handler " +
                    "decides what a link means; a screen only renders its route. That split is what " +
                    "keeps a deep link from becoming a second navigation system.",
            )
        },
    ) {
        HeroCard(
            title = "On its way",
            emoji = "🚚",
            accent = accent,
            subtitle = "Order #${route.id} · arrives Friday",
        )
        ContentCard {
            ListRow(title = "Ordered", emoji = "✅", accent = SamplePalette.Green, subtitle = "Mon 09:12")
            ListRow(title = "Packed", emoji = "✅", accent = SamplePalette.Green, subtitle = "Mon 16:40")
            ListRow(title = "Shipped", emoji = "🚚", accent = accent, subtitle = "Tue 08:05")
            ListRow(
                title = "Delivered",
                emoji = "⏳",
                accent = MaterialTheme.colorScheme.outline,
                subtitle = "Expected Friday",
            )
        }
    }
}
