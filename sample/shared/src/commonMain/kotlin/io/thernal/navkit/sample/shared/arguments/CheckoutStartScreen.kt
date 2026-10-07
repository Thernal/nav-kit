package io.thernal.navkit.sample.shared.arguments

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.ARGUMENTS.accent

/**
 * One value, set once, read and updated on four consecutive screens, and gone the moment the flow
 * leaves the stack — without being threaded through a single route.
 *
 * This is the requirement the argument store exists for, and the one a reference count cannot meet.
 */
@Composable
fun CheckoutStartScreen() {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    val leftOver = arguments.get(CheckoutDraftKey)

    SampleScreen(
        title = "Gift card",
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Buy a gift card",
                color = accent,
                onClick = {
                    arguments.put(
                        key = CheckoutDraftKey,
                        value = CheckoutDraft(),
                        scope = inCheckout,
                    )
                    navigator.push(CheckoutAmountRoute)
                },
            )
        },
        howItWorks = {
            LiveValue(
                label = "Draft outside the flow",
                value = leftOver?.toString() ?: "null — pruned when the flow left the stack",
            )
            Explanation(
                "Four steps share one `CheckoutDraft`, put as an argument scoped to the flow: " +
                    "`whileInStack { it is CheckoutStepRoute }`. Walk the flow and come back — the " +
                    "draft is gone, because the host prunes after every stack change. Nothing had " +
                    "to remember to clean it up.",
            )
        },
    ) {
        HeroCard(
            title = "Send a gift card",
            emoji = "🎁",
            accent = accent,
            subtitle = "Pick an amount, who it's for and how to pay — delivered by email in minutes.",
        )
    }
}
