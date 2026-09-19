package io.thernal.navkit.sample.basics

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.sample.catalog.CatalogRoute
import io.thernal.navkit.sample.ui.BottomAction
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.Green
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.StepHeader
import io.thernal.navkit.sample.ui.Topic

private const val LAST_STEP = 3

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

@Composable
fun WizardStepScreen(
    route: WizardStepRoute,
    log: OrderFlowLog,
) {
    val navigator = LocalNavigator.current
    val lastOutcome by log.last.collectAsState()
    val isLastStep = route.step >= LAST_STEP

    SampleScreen(
        title = "Checkout",
        topic = Topic.NAVIGATION,
        bottomBar = {
            if (isLastStep) {
                BottomAction(label = "Place order", onClick = { finish(navigator = navigator, log = log) })
            } else {
                BottomAction(
                    label = "Continue to ${stepLabel(route.step + 1)}",
                    onClick = {
                        val next = route.step + 1
                        log.record(
                            command = "push step $next",
                            outcome = navigator.push(WizardStepRoute(step = next)).describe(),
                        )
                    },
                )
            }
        },
        howItWorks = {
            LiveValue(label = "Last outcome", value = lastOutcome)
            Explanation(
                "*Edit delivery* uses `navigate` with a predicate: without it, it would push a " +
                    "second step 1; with it, the navigator finds the one already in the stack and " +
                    "pops down to it — one stack write, so guards see the destination actually asked for.",
            )
            Explanation(
                "*Cancel checkout* is `popBackTo`, which answers whether the stack moved and " +
                    "deliberately skips the back dispatcher — it is a jump, not a back. *Place order* " +
                    "is `replaceAll`: a whole new stack in one command, guarded on every route it proposes.",
            )
        },
    ) {
        StepHeader(current = route.step, total = LAST_STEP, label = stepLabel(route.step), accent = accent)
        StepDetails(step = route.step)

        SectionLabel(text = "Other ways to move")
        SecondaryButton(
            label = "Edit delivery · back to step 1",
            enabled = route.step > 1,
            onClick = {
                val outcome = navigator.navigate(
                    route = WizardStepRoute(step = 1),
                    predicate = { candidate -> candidate is WizardStepRoute && candidate.step == 1 },
                )
                log.record(command = "navigate to step 1", outcome = outcome.describe())
            },
        )
        SecondaryButton(
            label = "Cancel checkout",
            onClick = {
                val didMove = navigator.popBackTo { candidate -> candidate is WizardRoute }
                val outcome = if (didMove) {
                    "the stack moved"
                } else {
                    "the stack did not move"
                }
                log.record(command = "popBackTo the flow start", outcome = outcome)
            },
        )
        if (!isLastStep) {
            SecondaryButton(label = "Place order now", onClick = { finish(navigator = navigator, log = log) })
        }
    }
}

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
            accent = Green,
            subtitle = "Order #1042 is confirmed and arrives on Thursday.",
        )
        ContentCard {
            KeyValueRow(key = "Items", value = "2")
            KeyValueRow(key = "Delivery", value = "Standard · free")
            KeyValueRow(key = "Paid", value = "€42.00")
        }
    }
}

@Composable
private fun StepDetails(step: Int) {
    ContentCard {
        when (step) {
            1 -> {
                KeyValueRow(key = "Deliver to", value = "221B Baker Street")
                KeyValueRow(key = "Method", value = "Standard · 3–5 days")
            }

            2 -> {
                KeyValueRow(key = "Card", value = "Visa •••• 4242")
                KeyValueRow(key = "Billing", value = "Same as delivery")
            }

            else -> {
                KeyValueRow(key = "Items", value = "2")
                KeyValueRow(key = "Delivery", value = "Free")
                KeyValueRow(key = "Total", value = "€42.00")
            }
        }
    }
}

private fun stepLabel(step: Int): String {
    return when (step) {
        1 -> "Delivery"
        2 -> "Payment"
        else -> "Review"
    }
}

/**
 * A whole new stack in one command. `replaceAll` is guarded over every route it proposes, not just
 * the last one — the thing the kit fixed when guards moved from deciding about a route to deciding
 * about a stack.
 */
private fun finish(
    navigator: Navigator,
    log: OrderFlowLog,
) {
    val outcome = navigator.replaceAll(listOf(CatalogRoute, WizardDoneRoute))
    log.record(command = "replaceAll", outcome = outcome.describe())
}

private fun NavigationOutcome.describe(): String {
    return when (this) {
        is NavigationOutcome.Applied -> "applied · ${stack.size} route(s)"
        is NavigationOutcome.Rewritten -> "rewritten by a guard · ${reason?.message ?: "no reason given"}"
        is NavigationOutcome.Deferred -> "deferred · a guard needs time"
    }
}
