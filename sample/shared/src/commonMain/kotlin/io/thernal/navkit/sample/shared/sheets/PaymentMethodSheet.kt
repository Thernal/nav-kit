package io.thernal.navkit.sample.shared.sheets

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SheetStep
import io.thernal.navkit.sample.shared.ui.Topic

private data class SavedMethod(
    val label: String,
    val emoji: String,
    val detail: String,
)

private val savedMethods = listOf(
    SavedMethod(label = "Visa •••• 4242", emoji = "💳", detail = "Expires 04/29"),
    SavedMethod(label = "Pay by invoice", emoji = "🧾", detail = "14 days, business only"),
)

/** Step one: the saved methods, and the way into the run's second entry. */
@Composable
fun PaymentMethodSheet() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    SheetStep(title = "Pay €41", subtitle = "Choose a method") {
        savedMethods.forEach { saved ->
            ListRow(
                title = saved.label,
                emoji = saved.emoji,
                accent = Topic.BOTTOM_SHEETS.accent,
                subtitle = saved.detail,
                onClick = {
                    results.post(key = ChosenPaymentMethod, value = saved.label)
                    navigator.closeSheet()
                },
            )
        }
        ListRow(
            title = "Add a card",
            emoji = "➕",
            accent = Topic.BOTTOM_SHEETS.accent,
            subtitle = "Opens a second sheet on top of this one",
            // An ordinary push: the scene keeps the run in one panel rather than stacking two.
            onClick = { navigator.push(AddCardSheetRoute) },
        )
    }
}
