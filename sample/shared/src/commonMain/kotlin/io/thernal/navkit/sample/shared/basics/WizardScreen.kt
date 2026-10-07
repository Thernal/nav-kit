package io.thernal.navkit.sample.shared.basics

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.NAVIGATION.accent

/**
 * The commands a real flow needs beyond push and pop, and the thing that makes them usable: every
 * command that can add routes answers with a [NavigationOutcome] instead of `Unit`.
 *
 * That matters because a command is not a request that always succeeds. A guard can refuse it,
 * redirect it, or need time. Without an answer the call site cannot tell "we moved" from "we were
 * sent somewhere else" from "nothing happened", and the only report was an app-wide event stream
 * nobody at the call site was reading.
 *
 * The answers go to [OrderFlowLog] rather than to the step that asked: a command usually takes that
 * step off the screen, and the answer would go with it.
 */
@Composable
fun WizardScreen(log: OrderFlowLog) {
    val navigator = LocalNavigator.current
    val lastOutcome by log.last.collectAsState()

    SampleScreen(
        title = "Your bag",
        topic = Topic.NAVIGATION,
        bottomBar = {
            BottomAction(
                label = "Checkout · €42",
                onClick = {
                    log.reset()
                    log.record(
                        command = "push step 1",
                        outcome = navigator.push(WizardStepRoute(step = 1)).describe(),
                    )
                },
            )
        },
        howItWorks = {
            LiveValue(label = "Last outcome", value = lastOutcome)
            Explanation(
                "Every button in this flow is one navigator command, and the readout is what the " +
                    "command answered — the difference between a command surface and a setter.",
            )
        },
    ) {
        ContentCard {
            ListRow(title = "Ceramic mug", emoji = "☕", accent = accent, subtitle = "Qty 1", trailing = "€18")
            ListRow(title = "Linen tote", emoji = "👜", accent = accent, subtitle = "Qty 1", trailing = "€24")
            HorizontalDivider()
            KeyValueRow(key = "Total", value = "€42.00")
        }
    }
}
