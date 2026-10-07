package io.thernal.navkit.sample.shared.arguments

import androidx.compose.runtime.Composable

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
