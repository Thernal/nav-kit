package io.thernal.navkit.sample.basics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome
import io.thernal.navkit.sample.catalog.CatalogRoute
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

private const val LAST_STEP = 3

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

    ExampleScaffold(
        title = "Order flow",
        subtitle = "`navigate` with a predicate, `replaceAll`, `popBackTo`, and reading the outcome.",
    ) {
        ExampleReadout(label = "Last outcome", value = lastOutcome)
        ExampleNote(
            text = "Start the flow and walk forward. Every button reports what actually happened " +
                "to the stack, which is the difference between a command surface and a setter.",
        )
        ExampleAction(
            label = "Start at step 1",
            onClick = {
                log.reset()
                log.record(
                    command = "push step 1",
                    outcome = navigator.push(WizardStepRoute(step = 1)).describe(),
                )
            },
        )
    }
}

@Composable
fun WizardStepScreen(
    route: WizardStepRoute,
    log: OrderFlowLog,
) {
    val navigator = LocalNavigator.current
    val lastOutcome by log.last.collectAsState()

    ExampleScaffold(
        title = "Step ${route.step} of $LAST_STEP",
        subtitle = "Each button is one navigator command; the readout is its outcome.",
    ) {
        ExampleReadout(label = "Last outcome", value = lastOutcome)

        if (route.step < LAST_STEP) {
            ExampleAction(
                label = "Next step (push)",
                onClick = {
                    val next = route.step + 1
                    log.record(
                        command = "push step $next",
                        outcome = navigator.push(WizardStepRoute(step = next)).describe(),
                    )
                },
            )
        }

        ExampleAction(
            label = "Back to step 1 (navigate with a predicate)",
            onClick = {
                // Without the predicate this would push a *second* step 1. With it, the navigator
                // finds the one already in the stack and pops down to it — one stack write, so the
                // guards see the destination the caller actually asked for.
                val outcome = navigator.navigate(
                    route = WizardStepRoute(step = 1),
                    predicate = { candidate -> candidate is WizardStepRoute && candidate.step == 1 },
                )
                log.record(command = "navigate to step 1", outcome = outcome.describe())
            },
            enabled = route.step > 1,
        )

        ExampleAction(
            label = "Back to the flow start (popBackTo)",
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

        ExampleAction(
            label = "Finish (replaceAll)",
            onClick = {
                // A whole new stack in one command. `replaceAll` is guarded over every route it
                // proposes, not just the last one — the thing the kit fixed when guards moved from
                // deciding about a route to deciding about a stack.
                val outcome = navigator.replaceAll(listOf(CatalogRoute, WizardDoneRoute))
                log.record(command = "replaceAll", outcome = outcome.describe())
            },
        )

        ExampleNote(
            text = "`popBack` consults the host's back dispatcher first, so a screen intercepting " +
                "back is heard whether it came from the system gesture or from a button. " +
                "`popBackTo` deliberately does not: that is a jump, not a back. It answers whether " +
                "the stack moved; pass `inclusive = true` to pop the matched route as well.",
        )
    }
}

@Composable
fun WizardDoneScreen(log: OrderFlowLog) {
    val navigator = LocalNavigator.current
    val lastOutcome by log.last.collectAsState()

    ExampleScaffold(
        title = "Done",
        subtitle = "The stack is now catalog → done, built in one `replaceAll`.",
    ) {
        ExampleReadout(label = "Last outcome", value = lastOutcome)
        ExampleReadout(
            label = "Can pop?",
            value = navigator.canPop().toString(),
        )
        ExampleNote(
            text = "`canPop` answers from the stack the host is rendering, which is the resolved " +
                "one — after guards — not the one the caller last proposed.",
        )
    }
}

private fun NavigationOutcome.describe(): String {
    return when (this) {
        is NavigationOutcome.Applied -> "applied · ${stack.size} route(s)"
        is NavigationOutcome.Rewritten -> "rewritten by a guard · ${reason?.message ?: "no reason given"}"
        is NavigationOutcome.Deferred -> "deferred · a guard needs time"
    }
}
