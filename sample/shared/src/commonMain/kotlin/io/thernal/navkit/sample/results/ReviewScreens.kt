package io.thernal.navkit.sample.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.NavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

private const val REVIEW_STEPS = 3

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
    val pending by results.pending.collectAsState()
    var decision by remember { mutableStateOf<ReviewDecision?>(null) }

    ResultEffect(ReviewOutcome) { outcome -> decision = outcome }

    ExampleScaffold(
        title = "Review request",
        subtitle = "A three-step flow returns one decision to this screen.",
    ) {
        ExampleReadout(
            label = "Decision",
            value = decision?.let { made ->
                val verdict = if (made.isApproved) {
                    "approved"
                } else {
                    "rejected"
                }
                "$verdict — ${made.note}"
            } ?: "not reviewed yet",
        )
        ExampleReadout(
            label = "Pending result names",
            value = if (pending.isEmpty()) {
                "none"
            } else {
                pending.joinToString()
            },
        )
        ExampleAction(
            label = "Start the review",
            onClick = { navigator.push(ReviewStepRoute(step = 1)) },
        )
        ExampleAction(
            label = "Clear the decision",
            onClick = {
                results.clear(ReviewOutcome)
                decision = null
            },
            enabled = decision != null,
        )
        ExampleNote(
            text = "`pending` exposes names, never values, so one feature's results are not " +
                "readable by every screen in the app. Watch it while the flow is open: the name " +
                "appears the moment the last step posts and is gone the moment this screen " +
                "consumes it.",
        )
    }
}

@Composable
fun ReviewStepScreen(route: ReviewStepRoute) {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    ExampleScaffold(
        title = "Review step ${route.step} of $REVIEW_STEPS",
        subtitle = if (route.step < REVIEW_STEPS) {
            "Walk forward; the last step decides."
        } else {
            "Post the decision and leave the whole flow in one command."
        },
    ) {
        if (route.step < REVIEW_STEPS) {
            ExampleAction(
                label = "Continue",
                onClick = { navigator.push(ReviewStepRoute(step = route.step + 1)) },
            )
            return@ExampleScaffold
        }

        ExampleAction(
            label = "Approve",
            onClick = {
                finish(
                    approved = true,
                    results = results,
                    navigator = navigator,
                )
            },
        )
        ExampleAction(
            label = "Reject",
            onClick = {
                finish(
                    approved = false,
                    results = results,
                    navigator = navigator,
                )
            },
        )
        ExampleNote(
            text = "The flow posts and then pops itself with `popBackTo`, so the screen that " +
                "launched it is uncovered with the answer already waiting. `popBackTo` does not " +
                "consult the back dispatcher — it is a jump, not a back.",
        )
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
