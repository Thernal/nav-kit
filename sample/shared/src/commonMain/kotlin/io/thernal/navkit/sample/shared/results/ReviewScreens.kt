package io.thernal.navkit.sample.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.NavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.BottomAction
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.Green
import io.thernal.navkit.sample.ui.IconBadge
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.PrimaryButton
import io.thernal.navkit.sample.ui.Rose
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.StepHeader
import io.thernal.navkit.sample.ui.Topic

private const val REVIEW_STEPS = 3

private val accent = Topic.RESULTS.accent

/**
 * The shape a result actually takes in an application: a whole flow is launched, walks several
 * screens, and hands one value back to the screen that launched it — which by then is several
 * entries down the stack.
 *
 * This is also why the mailbox is application-scoped rather than per host. Host-scoping it would
 * read as tidier and would break precisely this case the moment the flow is mounted in a nested
 * host, because the screen waiting for the answer lives in the outer one.
 */
@Composable
fun ReviewHomeScreen() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current
    val model: ReviewHomeViewModel = viewModel { ReviewHomeViewModel() }
    val decision by model.decision.collectAsState()
    val pending by results.pending.collectAsState()

    // Read as this screen is composed, before `ResultEffect` consumes. Coming back from the flow is
    // the only moment a pending name can be seen from here: while the flow is open, this screen is
    // not composed at all.
    val pendingOnArrival = remember { results.pending.value }

    ResultEffect(key = ReviewOutcome, onResult = model::onDecision)

    SampleScreen(
        title = "Approvals",
        topic = Topic.RESULTS,
        howItWorks = {
            LiveValue(label = "Pending when this screen came back", value = pendingOnArrival.describe())
            LiveValue(label = "Pending now", value = pending.describe())
            Explanation(
                "The review is three screens deep; its last step posts one `ReviewDecision` and " +
                    "leaves the whole flow with `popBackTo`, so this screen is uncovered with the " +
                    "answer already waiting — compare the two readouts.",
            )
            Explanation(
                "`pending` exposes names, never values, so one feature's results are not " +
                    "readable by every screen in the app.",
            )
        },
    ) {
        ContentCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(emoji = "🧾", color = accent)
                Text(
                    text = "Team offsite — Lisbon",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                DecisionChip(decision = decision)
            }
            KeyValueRow(key = "Submitted by", value = "Maya Chen")
            KeyValueRow(key = "Amount", value = "€840.20")
            if (decision != null) {
                KeyValueRow(key = "Reviewer note", value = decision?.note.orEmpty())
            }
        }
        PrimaryButton(
            label = "Review expense report",
            onClick = { navigator.push(ReviewStepRoute(step = 1)) },
            color = accent,
        )
        SecondaryButton(
            label = "Reset the decision",
            enabled = decision != null,
            onClick = {
                results.clear(ReviewOutcome)
                model.clear()
            },
        )
    }
}

@Composable
fun ReviewStepScreen(route: ReviewStepRoute) {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current
    val isDecisionStep = route.step >= REVIEW_STEPS

    SampleScreen(
        title = "Review",
        topic = Topic.RESULTS,
        bottomBar = {
            if (!isDecisionStep) {
                BottomAction(
                    label = "Continue",
                    color = accent,
                    onClick = { navigator.push(ReviewStepRoute(step = route.step + 1)) },
                )
            }
        },
        howItWorks = {
            Explanation(
                if (isDecisionStep) {
                    "Approve or reject posts the decision and leaves the whole flow in one " +
                        "`popBackTo` — a jump, not a back, so it does not consult the back " +
                        "dispatcher."
                } else {
                    "Each step is an ordinary pushed route. Nothing is threaded forward: the " +
                        "decision only exists once the last step makes it."
                },
            )
        },
    ) {
        StepHeader(current = route.step, total = REVIEW_STEPS, label = stepLabel(route.step), accent = accent)
        when (route.step) {
            1 -> ContentCard {
                ListRow(title = "Hotel · 3 nights", emoji = "🏨", accent = accent, trailing = "€520.00")
                ListRow(title = "Flights", emoji = "✈️", accent = accent, trailing = "€260.20")
                ListRow(title = "Team dinner", emoji = "🍽️", accent = accent, trailing = "€60.00")
            }

            2 -> ContentCard {
                ListRow(title = "Within the offsite budget", emoji = "✅", accent = Green)
                ListRow(title = "Every receipt attached", emoji = "✅", accent = Green)
                ListRow(title = "Approver is not the submitter", emoji = "✅", accent = Green)
            }

            else -> {
                ContentCard {
                    KeyValueRow(key = "Receipts", value = "3 of 3")
                    KeyValueRow(key = "Policy checks", value = "Passed")
                    HorizontalDivider()
                    KeyValueRow(key = "Total", value = "€840.20")
                }
                PrimaryButton(
                    label = "Approve",
                    color = Green,
                    onClick = { finish(approved = true, results = results, navigator = navigator) },
                )
                PrimaryButton(
                    label = "Reject",
                    color = Rose,
                    onClick = { finish(approved = false, results = results, navigator = navigator) },
                )
            }
        }
    }
}

@Composable
private fun DecisionChip(decision: ReviewDecision?) {
    when {
        decision == null -> StatusChip(text = "Pending", color = MaterialTheme.colorScheme.tertiary)
        decision.isApproved -> StatusChip(text = "Approved", color = Green)
        else -> StatusChip(text = "Rejected", color = Rose)
    }
}

private fun stepLabel(step: Int): String {
    return when (step) {
        1 -> "Receipts"
        2 -> "Policy"
        else -> "Decision"
    }
}

private fun Set<String>.describe(): String {
    if (isEmpty()) {
        return "none"
    }
    return joinToString()
}

private fun finish(
    approved: Boolean,
    results: NavigationResults,
    navigator: Navigator,
) {
    val note = if (approved) {
        "looks good"
    } else {
        "needs changes"
    }
    results.post(
        key = ReviewOutcome,
        value = ReviewDecision(isApproved = approved, note = note),
    )
    navigator.popBackTo { candidate -> candidate is ReviewHomeRoute }
}
