package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.DEEP_LINKS.accent

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
        ListRow(title = "Order #76", emoji = "✅", accent = SamplePalette.Green, subtitle = "Delivered 2 Sep")
        ListRow(title = "Order #75", emoji = "✅", accent = SamplePalette.Green, subtitle = "Delivered 18 Aug")
    }
}
