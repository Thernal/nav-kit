package io.thernal.navkit.sample.shared.sheets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.SheetStep
import io.thernal.navkit.sample.shared.ui.StatusChip
import io.thernal.navkit.sample.shared.ui.Topic

/** Step three: one result for the whole run, then one command to close it. */
@Composable
fun ConfirmCardSheet(route: ConfirmCardSheetRoute) {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    SheetStep(title = "Confirm", subtitle = "Step 3 of 3") {
        StatusChip(text = "NEW CARD", color = Topic.BOTTOM_SHEETS.accent)
        Text(text = route.maskedNumber, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "The card is saved to this basket only. Nothing leaves the device — this is a " +
                "navigation sample.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ListRow(
            title = "Save and pay €41",
            emoji = "✅",
            accent = Topic.BOTTOM_SHEETS.accent,
            subtitle = "Posts the result, then closes all three steps",
            onClick = {
                results.post(key = ChosenPaymentMethod, value = route.maskedNumber)
                navigator.closeSheet()
            },
        )
        SecondaryButton(label = "Back", onClick = { navigator.popBack() })
    }
}
