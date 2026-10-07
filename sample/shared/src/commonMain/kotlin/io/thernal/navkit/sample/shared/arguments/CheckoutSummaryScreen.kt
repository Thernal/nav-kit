package io.thernal.navkit.sample.shared.arguments

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.StepHeader
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.ARGUMENTS.accent

@Composable
fun CheckoutSummaryScreen() {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    val draft = arguments.get(CheckoutDraftKey)

    SampleScreen(
        title = "Summary",
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Confirm",
                color = accent,
                onClick = { navigator.popBackTo { candidate -> candidate is CheckoutStartRoute } },
            )
        },
        howItWorks = {
            Explanation(
                "The fourth screen reads what the first three wrote. Confirming pops every " +
                    "`CheckoutStepRoute` at once; the next stack change finds none of them alive, " +
                    "so the draft is dropped — not by this screen, by the scope.",
            )
        },
    ) {
        StepHeader(
            current = CheckoutStepRoute.COUNT,
            total = CheckoutStepRoute.COUNT,
            label = "Summary",
            accent = accent,
        )
        ContentCard {
            KeyValueRow(key = "Gift card", value = draft?.amount.orPlaceholder { amount -> "€$amount" })
            KeyValueRow(key = "To", value = draft?.address.orPlaceholder())
            KeyValueRow(key = "Paid with", value = draft?.method.orPlaceholder())
            HorizontalDivider()
            KeyValueRow(key = "Total", value = draft?.amount.orPlaceholder { amount -> "€$amount" })
        }
    }
}

private fun String?.orPlaceholder(format: (String) -> String = { it }): String {
    if (isNullOrBlank()) {
        return "—"
    }
    return format(this)
}
