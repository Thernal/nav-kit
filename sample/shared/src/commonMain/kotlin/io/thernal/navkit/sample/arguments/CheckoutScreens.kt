package io.thernal.navkit.sample.arguments

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

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

    ExampleScaffold(
        title = "Checkout",
        subtitle = "Four steps share one draft, scoped to the flow rather than to a screen.",
    ) {
        ExampleReadout(
            label = "Draft outside the flow",
            value = leftOver?.toString() ?: "null — pruned when the flow left the stack",
        )
        ExampleAction(
            label = "Start checkout",
            onClick = {
                arguments.put(
                    key = CheckoutDraftKey,
                    value = CheckoutDraft(),
                    scope = whileInStack { it is CheckoutStepRoute },
                )
                navigator.push(CheckoutAmountRoute)
            },
        )
        ExampleNote(
            text = "Walk the flow, then come back here. The draft is gone: its scope says it is " +
                "alive while any CheckoutStepRoute is in the stack, and the host prunes after " +
                "every stack change. Nothing had to remember to clean it up.",
        )
    }
}

@Composable
fun CheckoutAmountScreen() {
    CheckoutStep(
        title = "Amount",
        label = "How much",
        read = { draft -> draft.amount },
        write = { draft, entered -> draft.copy(amount = entered) },
        next = CheckoutAddressRoute,
    )
}

@Composable
fun CheckoutAddressScreen() {
    CheckoutStep(
        title = "Address",
        label = "Ship to",
        read = { draft -> draft.address },
        write = { draft, entered -> draft.copy(address = entered) },
        next = CheckoutPaymentRoute,
    )
}

@Composable
fun CheckoutPaymentScreen() {
    CheckoutStep(
        title = "Payment",
        label = "Method",
        read = { draft -> draft.method },
        write = { draft, entered -> draft.copy(method = entered) },
        next = CheckoutSummaryRoute,
    )
}

@Composable
fun CheckoutSummaryScreen() {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    val draft = arguments.get(CheckoutDraftKey)

    ExampleScaffold(
        title = "Summary",
        subtitle = "The fourth screen reads what the first three wrote.",
    ) {
        ExampleReadout(label = "Amount", value = draft?.amount.orPlaceholder())
        ExampleReadout(label = "Address", value = draft?.address.orPlaceholder())
        ExampleReadout(label = "Method", value = draft?.method.orPlaceholder())
        ExampleAction(
            label = "Leave the flow",
            onClick = { navigator.popBackTo { candidate -> candidate is CheckoutStartRoute } },
        )
        ExampleNote(
            text = "Leaving pops every CheckoutStepRoute at once. The next stack change finds " +
                "none of them alive, so the draft is dropped — not by this screen, by the scope.",
        )
    }
}

/**
 * The steps differ only in which field they touch, so they share one body. A real flow would look
 * the same: the argument is read and written through the same key on every screen.
 */
@Composable
private fun CheckoutStep(
    title: String,
    label: String,
    read: (CheckoutDraft) -> String,
    write: (CheckoutDraft, String) -> CheckoutDraft,
    next: CheckoutStepRoute,
) {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    val draft = arguments.get(CheckoutDraftKey) ?: CheckoutDraft()

    ExampleScaffold(
        title = title,
        subtitle = "Reads the shared draft, writes one field back into it.",
    ) {
        OutlinedTextField(
            value = read(draft),
            onValueChange = { entered ->
                arguments.update(write(draft, entered))
            },
            label = { Text(text = label) },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleAction(
            label = "Continue",
            onClick = { navigator.push(next) },
        )
    }
}

/** Re-putting under the same key replaces the value and keeps the scope it was given. */
private fun NavigationArguments.update(draft: CheckoutDraft) {
    put(
        key = CheckoutDraftKey,
        value = draft,
        scope = whileInStack { it is CheckoutStepRoute },
    )
}

private fun String?.orPlaceholder(): String {
    if (isNullOrBlank()) {
        return "—"
    }
    return this
}
