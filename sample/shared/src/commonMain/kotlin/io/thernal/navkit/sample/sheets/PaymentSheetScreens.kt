package io.thernal.navkit.sample.sheets

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.BottomAction
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.SheetStep
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.Topic

private data class BasketLine(
    val name: String,
    val emoji: String,
    val price: String,
)

private val basketLines = listOf(
    BasketLine(name = "Rye starter kit", emoji = "🌾", price = "€22"),
    BasketLine(name = "Banneton, 25 cm", emoji = "🧺", price = "€19"),
)

private data class SavedMethod(
    val label: String,
    val emoji: String,
    val detail: String,
)

private val savedMethods = listOf(
    SavedMethod(label = "Visa •••• 4242", emoji = "💳", detail = "Expires 04/29"),
    SavedMethod(label = "Pay by invoice", emoji = "🧾", detail = "14 days, business only"),
)

private val cardBrands = listOf("Visa" to "4113", "Mastercard" to "8421", "Amex" to "3007")

/**
 * A sheet that pushes another sheet, and another after that: three entries the scene claims as one
 * panel, pushed with `push` like any other route.
 */
@Composable
fun BasketScreen() {
    val navigator = LocalNavigator.current
    var method by rememberSaveable { mutableStateOf<String?>(null) }
    var isPaid by rememberSaveable { mutableStateOf(false) }

    ResultEffect(ChosenPaymentMethod) { chosen ->
        method = chosen
        isPaid = false
    }

    SampleScreen(
        title = "Basket",
        topic = Topic.BOTTOM_SHEETS,
        howItWorks = {
            Explanation(
                "The scene claims the *run* of consecutive sheet entries at the top of the stack, so " +
                    "*Choose a method* → *Add a card* → *Confirm* is one panel with a back stack of " +
                    "its own. Back inside the run returns to the previous step; back on the first " +
                    "step closes the sheet.",
            )
            Explanation(
                "Closing from the third step is one command — `popBackTo { it !is SheetStepRoute }` " +
                    "in `closeSheet()`. The marker interface exists for exactly that: the sheet does " +
                    "not have to know how many steps the user opened, or which.",
            )
            Explanation(
                "`ConfirmCardSheetRoute(maskedNumber)` carries what the step before it chose. A sheet " +
                    "step is an ordinary route, so a property on it is the ordinary way to hand a " +
                    "value forwards — and the final step posts one result back for the whole run.",
            )
            Explanation(
                "Watch the row below while the sheet is open: an overlay leaves the screen under it " +
                    "composed, so the chosen method appears there before the panel is dismissed.",
            )
        },
        bottomBar = {
            BottomAction(
                label = if (isPaid) {
                    "Paid"
                } else {
                    "Pay €41"
                },
                onClick = { isPaid = true },
                enabled = method != null && !isPaid,
                color = Topic.BOTTOM_SHEETS.accent,
            )
        },
    ) {
        SectionLabel(text = "2 items")
        ContentCard {
            basketLines.forEach { line ->
                KeyValueRow(key = "${line.emoji}  ${line.name}", value = line.price)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            KeyValueRow(key = "Total", value = "€41")
        }
        SectionLabel(text = "Payment")
        ListRow(
            title = "Payment method",
            emoji = "💳",
            accent = Topic.BOTTOM_SHEETS.accent,
            subtitle = method ?: "None chosen",
            onClick = { navigator.push(PaymentMethodSheetRoute) },
        )
    }
}

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
