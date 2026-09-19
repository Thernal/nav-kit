package io.thernal.navkit.sample.arguments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentScope
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.BottomAction
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.StepHeader
import io.thernal.navkit.sample.ui.Topic

private const val CHECKOUT_STEPS = 4

private val accent = Topic.ARGUMENTS.accent

/** Alive while any step of the flow is on the stack — the start screen is not a step. */
private val inCheckout: ArgumentScope = whileInStack { route -> route is CheckoutStepRoute }

/**
 * One value, set once, read and updated on four consecutive screens, and gone the moment the flow
 * leaves the stack — without being threaded through a single route.
 *
 * This is the requirement the argument store exists for, and the one a reference count cannot meet.
 */
@Composable
fun CheckoutStartScreen() {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    val leftOver = arguments.get(CheckoutDraftKey)

    SampleScreen(
        title = "Gift card",
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Buy a gift card",
                color = accent,
                onClick = {
                    arguments.put(
                        key = CheckoutDraftKey,
                        value = CheckoutDraft(),
                        scope = inCheckout,
                    )
                    navigator.push(CheckoutAmountRoute)
                },
            )
        },
        howItWorks = {
            LiveValue(
                label = "Draft outside the flow",
                value = leftOver?.toString() ?: "null — pruned when the flow left the stack",
            )
            Explanation(
                "Four steps share one `CheckoutDraft`, put as an argument scoped to the flow: " +
                    "`whileInStack { it is CheckoutStepRoute }`. Walk the flow and come back — the " +
                    "draft is gone, because the host prunes after every stack change. Nothing had " +
                    "to remember to clean it up.",
            )
        },
    ) {
        HeroCard(
            title = "Send a gift card",
            emoji = "🎁",
            accent = accent,
            subtitle = "Pick an amount, who it's for and how to pay — delivered by email in minutes.",
        )
    }
}

@Composable
fun CheckoutAmountScreen() {
    CheckoutStep(
        step = 1,
        title = "Amount",
        label = "Gift amount (€)",
        read = { draft -> draft.amount },
        write = { draft, entered -> draft.copy(amount = entered) },
        next = CheckoutAddressRoute,
    )
}

@Composable
fun CheckoutAddressScreen() {
    CheckoutStep(
        step = 2,
        title = "Recipient",
        label = "Recipient's email",
        read = { draft -> draft.address },
        write = { draft, entered -> draft.copy(address = entered) },
        next = CheckoutPaymentRoute,
    )
}

@Composable
fun CheckoutPaymentScreen() {
    CheckoutStep(
        step = 3,
        title = "Payment",
        label = "Pay with",
        read = { draft -> draft.method },
        write = { draft, entered -> draft.copy(method = entered) },
        next = CheckoutSummaryRoute,
        choices = listOf("Card", "Apple Pay", "PayPal"),
    )
}

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
        StepHeader(current = CHECKOUT_STEPS, total = CHECKOUT_STEPS, label = "Summary", accent = accent)
        ContentCard {
            KeyValueRow(key = "Gift card", value = draft?.amount.orPlaceholder { amount -> "€$amount" })
            KeyValueRow(key = "To", value = draft?.address.orPlaceholder())
            KeyValueRow(key = "Paid with", value = draft?.method.orPlaceholder())
            HorizontalDivider()
            KeyValueRow(key = "Total", value = draft?.amount.orPlaceholder { amount -> "€$amount" })
        }
    }
}

/**
 * The steps differ only in which field they touch, so they share one body. A real flow would look
 * the same: the argument is read and written through the same key on every screen.
 *
 * The field keeps its own state and writes every change through to the argument. The argument
 * store is not snapshot state, so a field whose value came from it never recomposed: every
 * keystroke was reverted, and the revert was written back as an empty string. It is read once,
 * when the step is first shown, and saved with the entry after that.
 */
@Composable
private fun CheckoutStep(
    step: Int,
    title: String,
    label: String,
    read: (CheckoutDraft) -> String,
    write: (CheckoutDraft, String) -> CheckoutDraft,
    next: CheckoutStepRoute,
    choices: List<String> = emptyList(),
) {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    var entered by rememberSaveable { mutableStateOf(read(arguments.draft())) }
    val onEntered: (String) -> Unit = { text ->
        entered = text
        arguments.update { draft -> write(draft, text) }
    }

    SampleScreen(
        title = title,
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Continue",
                color = accent,
                enabled = entered.isNotBlank(),
                onClick = { navigator.push(next) },
            )
        },
        howItWorks = {
            Explanation(
                "Reads the shared draft and writes this one field back into it, under the same " +
                    "key on every step. Go back a step and the value is still there — the draft " +
                    "lives as long as any step of the flow is on the stack.",
            )
        },
    ) {
        StepHeader(current = step, total = CHECKOUT_STEPS, label = title, accent = accent)
        if (choices.isEmpty()) {
            OutlinedTextField(
                value = entered,
                onValueChange = onEntered,
                label = { Text(text = label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(text = label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice ->
                    FilterChip(
                        selected = entered == choice,
                        onClick = { onEntered(choice) },
                        label = { Text(text = choice) },
                    )
                }
            }
        }
    }
}

private fun NavigationArguments.draft(): CheckoutDraft {
    return get(CheckoutDraftKey) ?: CheckoutDraft()
}

/**
 * Re-putting under the same key replaces the value. [change] is applied to the draft as it is now,
 * not as the screen last saw it, so two steps never write over each other's fields.
 */
private fun NavigationArguments.update(change: (CheckoutDraft) -> CheckoutDraft) {
    put(
        key = CheckoutDraftKey,
        value = change(draft()),
        scope = inCheckout,
    )
}

private fun String?.orPlaceholder(format: (String) -> String = { it }): String {
    if (isNullOrBlank()) {
        return "—"
    }
    return format(this)
}
