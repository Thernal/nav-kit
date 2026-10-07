package io.thernal.navkit.sample.shared.basics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.NAVIGATION.accent

@Composable
fun WizardDoneScreen(log: OrderFlowLog) {
    val navigator = LocalNavigator.current
    val lastOutcome by log.last.collectAsState()

    SampleScreen(
        title = "Order placed",
        topic = Topic.NAVIGATION,
        howItWorks = {
            LiveValue(label = "Last outcome", value = lastOutcome)
            LiveValue(label = "Can pop?", value = navigator.canPop().toString())
            Explanation(
                "The stack is now catalog → this screen, built in one `replaceAll`, so back goes " +
                    "to the catalog rather than into a checkout that no longer exists. `canPop` " +
                    "answers from the stack the host is rendering — after guards — not the one the " +
                    "caller last proposed.",
            )
        },
    ) {
        HeroCard(
            title = "Thank you!",
            emoji = "✅",
            accent = SamplePalette.Green,
            subtitle = "Order #1042 is confirmed and arrives on Thursday.",
        )
        ContentCard {
            KeyValueRow(key = "Items", value = "2")
            KeyValueRow(key = "Delivery", value = "Standard · free")
            KeyValueRow(key = "Paid", value = "€42.00")
        }
    }
}
