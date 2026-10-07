package io.thernal.navkit.sample.shared.basics

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

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
        ShopItem.all.forEach { item ->
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
