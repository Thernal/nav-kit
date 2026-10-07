package io.thernal.navkit.sample.shared.basics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.sample.shared.catalog.CatalogRoute
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.StepHeader
import io.thernal.navkit.sample.shared.ui.Topic

private const val LAST_STEP = 3

private val accent = Topic.NAVIGATION.accent

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
