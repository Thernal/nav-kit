package io.thernal.navkit.sample.shared.arguments

import androidx.compose.runtime.Composable

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
