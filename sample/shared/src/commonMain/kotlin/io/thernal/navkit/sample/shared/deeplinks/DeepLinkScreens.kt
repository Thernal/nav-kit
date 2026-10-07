package io.thernal.navkit.sample.deeplinks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.app.DeepLinkLog
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.DemoButton
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.Green
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.Rose
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.Topic

private val accent = Topic.DEEP_LINKS.accent

private val presets = listOf(
    "App scheme" to "navkit://product/42",
    "Web" to "https://example.com/product/42",
    "Other domain" to "https://elsewhere.example/product/42",
)

/**
 * Publishing a link by hand, which is all an activity's `onNewIntent` or an iOS URL callback does.
 * Everything after that is the application's, and none of it is on this screen — except the answer,
 * which the root logs so a link that went nowhere says why.
 */
@Composable
fun LinkPlaygroundScreen(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
) {
    var uri by rememberSaveable { mutableStateOf("navkit://product/42") }
    val lastResult by log.last.collectAsState()

    SampleScreen(
        title = "Link tester",
        topic = Topic.DEEP_LINKS,
        subtitle = "Deliver a link the way the platform would, and watch the stack change.",
        howItWorks = {
            Explanation(
                "Both registered forms reach the same handler: the parser removes whichever " +
                    "registered base a link starts with — `navkit://` or `https://example.com` — " +
                    "and what follows is the page. A feature declares its page once. A link on any " +
                    "other domain starts with no registered base and is not found.",
            )
            Explanation(
                "The platform delivers the same way: `adb shell am start -d navkit://product/7` " +
                    "on Android, `xcrun simctl openurl booted navkit://product/7` on iOS.",
            )
        },
    ) {
        LinkResult(lastResult = lastResult)
        OutlinedTextField(
            value = uri,
            onValueChange = { entered -> uri = entered },
            label = { Text(text = "Link") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (label, preset) ->
                AssistChip(
                    onClick = {
                        uri = preset
                        deliver(ingress = ingress, log = log, uri = uri)
                    },
                    label = { Text(text = label) },
                )
            }
        }
        DemoButton(
            label = "Deliver this link",
            onClick = { deliver(ingress = ingress, log = log, uri = uri) },
        )
    }
}

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

@Composable
fun LinkCampaignScreen(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
) {
    val lastResult by log.last.collectAsState()

    SampleScreen(
        title = "Notifications",
        topic = Topic.DEEP_LINKS,
        howItWorks = {
            Explanation(
                "Each notification is a link with a source. The shipped-order one lands three " +
                    "screens deep: the handler returns catalog → campaign → orders → order 77, so " +
                    "back walks the list instead of leaving the app.",
            )
            Explanation(
                "The in-app one stays here and says why: the ingress accepted it, the handler " +
                    "refused its source. The members offer is the interesting case — signed out, " +
                    "the link resolves to the lounge and the guard rewrites it to a sign-in screen " +
                    "before anything renders, and the root that applied the link never had to ask.",
            )
        },
    ) {
        LinkResult(lastResult = lastResult)
        SectionLabel(text = "Tap a notification to open its link")
        ListRow(
            title = "Your orders",
            emoji = "📦",
            accent = accent,
            subtitle = "Link · navkit://orders",
            onClick = { deliver(ingress = ingress, log = log, uri = "navkit://orders") },
        )
        ListRow(
            title = "Order #77 has shipped",
            emoji = "🚚",
            accent = accent,
            subtitle = "Link · navkit://orders/77",
            onClick = { deliver(ingress = ingress, log = log, uri = "navkit://orders/77") },
        )
        ListRow(
            title = "Order #77 — in-app banner",
            emoji = "🔔",
            accent = Rose,
            subtitle = "In-app notification · rejected by its handler",
            onClick = {
                deliver(
                    ingress = ingress,
                    log = log,
                    uri = "navkit://orders/77",
                    source = DeepLinkSource.IN_APP_NOTIFICATION,
                )
            },
        )
        ListRow(
            title = "Members-only offer",
            emoji = "⭐",
            accent = Topic.GUARDS.accent,
            subtitle = "Push notification · navkit://secret, a guarded page",
            onClick = {
                deliver(
                    ingress = ingress,
                    log = log,
                    uri = "navkit://secret",
                    source = DeepLinkSource.PUSH_NOTIFICATION,
                )
            },
        )
    }
}

@Composable
fun OrdersScreen() {
    val navigator = LocalNavigator.current
    SampleScreen(
        title = "Orders",
        topic = Topic.DEEP_LINKS,
        howItWorks = {
            Explanation("The middle of a link-built stack: here because the link put it here, below the order.")
        },
    ) {
        ListRow(
            title = "Order #77",
            emoji = "🚚",
            accent = accent,
            subtitle = "Shipped · arrives Friday",
            onClick = { navigator.push(OrderRoute(id = "77")) },
        )
        ListRow(title = "Order #76", emoji = "✅", accent = Green, subtitle = "Delivered 2 Sep")
        ListRow(title = "Order #75", emoji = "✅", accent = Green, subtitle = "Delivered 18 Aug")
    }
}

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
            ListRow(title = "Ordered", emoji = "✅", accent = Green, subtitle = "Mon 09:12")
            ListRow(title = "Packed", emoji = "✅", accent = Green, subtitle = "Mon 16:40")
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

/** What became of the last link — the answer the root logged, not the one the ingress gave. */
@Composable
private fun LinkResult(lastResult: String?) {
    ContentCard {
        val (status, color) = when {
            lastResult == null -> "No link delivered yet" to MaterialTheme.colorScheme.onSurfaceVariant
            "opened" in lastResult -> "Opened" to Green
            else -> "Not opened" to Rose
        }
        StatusChip(text = status, color = color)
        Text(
            text = lastResult ?: "Deliver a link to see what its handler decided.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/**
 * Hands [uri] to the ingress. What a handler makes of it arrives later, through [log]; only a link
 * the ingress itself turns away is recorded here.
 */
private fun deliver(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
    uri: String,
    source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
) {
    if (!ingress.publish(uri = uri, source = source)) {
        log.recordRefused(uri)
    }
}
