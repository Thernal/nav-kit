package io.thernal.navkit.sample.shared.results

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.NavigationResults
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.PrimaryButton
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.StepHeader
import io.thernal.navkit.sample.shared.ui.Topic

private const val REVIEW_STEPS = 3

private val accent = Topic.RESULTS.accent

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
                ListRow(title = "Within the offsite budget", emoji = "✅", accent = SamplePalette.Green)
                ListRow(title = "Every receipt attached", emoji = "✅", accent = SamplePalette.Green)
                ListRow(title = "Approver is not the submitter", emoji = "✅", accent = SamplePalette.Green)
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
                    color = SamplePalette.Green,
                    onClick = { finish(approved = true, results = results, navigator = navigator) },
                )
                PrimaryButton(
                    label = "Reject",
                    color = SamplePalette.Rose,
                    onClick = { finish(approved = false, results = results, navigator = navigator) },
                )
            }
        }
    }
}

private fun stepLabel(step: Int): String {
    return when (step) {
        1 -> "Receipts"
        2 -> "Policy"
        else -> "Decision"
    }
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
