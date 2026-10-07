package io.thernal.navkit.sample.shared.arguments

import androidx.compose.runtime.Composable

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
