package io.thernal.navkit.sample.shared.basics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private fun shopItemOf(id: String): ShopItem {
    return ShopItem.all.firstOrNull { item -> item.id == id }
        ?: ShopItem(id = id, emoji = "📦", name = "Item $id", blurb = "", price = "—", material = "—")
}

@Composable
fun BasicsDetailScreen(route: BasicsDetailRoute) {
    val item = shopItemOf(route.id)
    SampleScreen(
        title = item.name,
        topic = Topic.NAVIGATION,
        howItWorks = {
            Explanation(
                "The route itself carries the id — `BasicsDetailRoute(id = \"${route.id}\")` — no " +
                    "shared state, no store. A route is a small immutable value that survives " +
                    "process death; anything bigger than an identifier belongs in a repository, " +
                    "with only its id here.",
            )
        },
    ) {
        HeroCard(
            title = item.name,
            emoji = item.emoji,
            accent = Topic.NAVIGATION.accent,
            subtitle = item.blurb,
        )
        ContentCard {
            KeyValueRow(key = "Price", value = item.price)
            KeyValueRow(key = "Material", value = item.material)
            KeyValueRow(key = "Ships in", value = "2–3 days")
            Text(
                text = "Made in small batches. Every piece is a little different.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
