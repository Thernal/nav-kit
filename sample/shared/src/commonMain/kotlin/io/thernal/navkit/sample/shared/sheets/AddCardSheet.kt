package io.thernal.navkit.sample.shared.sheets

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.SheetStep
import io.thernal.navkit.sample.shared.ui.Topic

private val cardBrands = listOf("Visa" to "4113", "Mastercard" to "8421", "Amex" to "3007")

/** Step two. Back from here returns to step one rather than closing the sheet. */
@Composable
fun AddCardSheet() {
    val navigator = LocalNavigator.current

    SheetStep(title = "Add a card", subtitle = "Step 2 of 3 · back returns to the list") {
        cardBrands.forEach { (brand, digits) ->
            ListRow(
                title = brand,
                emoji = "💳",
                accent = Topic.BOTTOM_SHEETS.accent,
                subtitle = "•••• $digits",
                onClick = {
                    navigator.push(ConfirmCardSheetRoute(maskedNumber = "$brand •••• $digits"))
                },
            )
        }
        SecondaryButton(label = "Cancel", onClick = { navigator.closeSheet() })
    }
}
