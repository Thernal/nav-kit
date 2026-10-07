package io.thernal.navkit.sample.basics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.Topic

/** What the shop shows for a detail id. Presentation only — the route still carries just the id. */
private data class ShopItem(
    val id: String,
    val emoji: String,
    val name: String,
    val blurb: String,
    val price: String,
    val material: String,
)

private val shopItems = listOf(
    ShopItem(
        id = "A",
        emoji = "☕",
        name = "Ceramic mug",
        blurb = "Hand-glazed stoneware, 350 ml",
        price = "€18",
        material = "Stoneware",
    ),
    ShopItem(
        id = "B",
        emoji = "👜",
        name = "Linen tote",
        blurb = "Natural linen, fits a 14\" laptop",
        price = "€24",
        material = "Linen",
    ),
)

private fun shopItemOf(id: String): ShopItem {
    return shopItems.firstOrNull { item -> item.id == id }
        ?: ShopItem(id = id, emoji = "📦", name = "Item $id", blurb = "", price = "—", material = "—")
}

/**
 * The whole of simple navigation: a screen pushes another and the other pops.
 *
 * Nothing is injected and nothing is registered. `LocalNavigator` is provided by the mounted host,
 * so a screen reaches it without a constructor parameter and without knowing which host it is in —
 * a nested host provides its own, and the same screen then drives that one instead.
 */
@Composable
fun BasicsHomeScreen() {
    val navigator = LocalNavigator.current
    SampleScreen(
        title = "Shop",
        topic = Topic.NAVIGATION,
        subtitle = "Tap an item to push its page; back pops it.",
        howItWorks = {
            Explanation(
                "`push` adds a route; `popBack` removes the top one. The host renders the " +
                    "stack the root ViewModel owns, so both commands are reported back to it rather " +
                    "than mutating anything the host holds.",
            )
        },
    ) {
        SectionLabel(text = "Popular this week")
        shopItems.forEach { item ->
            ListRow(
                title = item.name,
                emoji = item.emoji,
                accent = Topic.NAVIGATION.accent,
                subtitle = item.blurb,
                trailing = item.price,
                onClick = { navigator.push(BasicsDetailRoute(id = item.id)) },
            )
        }
    }
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
