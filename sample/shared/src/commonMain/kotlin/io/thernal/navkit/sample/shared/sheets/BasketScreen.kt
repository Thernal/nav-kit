package io.thernal.navkit.sample.shared.sheets

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.shared.ui.BottomAction
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.KeyValueRow
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private data class BasketLine(
    val name: String,
    val emoji: String,
    val price: String,
)

private val basketLines = listOf(
    BasketLine(name = "Rye starter kit", emoji = "🌾", price = "€22"),
    BasketLine(name = "Banneton, 25 cm", emoji = "🧺", price = "€19"),
)

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
