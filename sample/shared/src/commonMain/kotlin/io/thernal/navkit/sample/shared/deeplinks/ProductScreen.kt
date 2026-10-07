package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.runtime.Composable
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.DEEP_LINKS.accent

@Composable
fun ProductScreen(route: ProductRoute) {
    SampleScreen(
        title = "Product",
        topic = Topic.DEEP_LINKS,
        howItWorks = {
            Explanation(
                "Opened by a link. The handler returned a whole stack — the catalog, the tester " +
                    "and this page — not a destination. That is why back from here goes somewhere " +
                    "sensible instead of closing the app.",
            )
        },
    ) {
        HeroCard(
            title = "Studio headphones",
            emoji = "🎧",
            accent = accent,
            subtitle = "Product ${route.id} · noise cancelling, 30 h battery",
        )
        ContentCard {
            KeyValueRow(key = "Price", value = "€199")
            KeyValueRow(key = "Rating", value = "★ 4.8 (2,140)")
            KeyValueRow(key = "Delivery", value = "Tomorrow")
        }
    }
}
