package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.sample.shared.app.DeepLinkLog
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.DEEP_LINKS.accent

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
            accent = SamplePalette.Rose,
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
