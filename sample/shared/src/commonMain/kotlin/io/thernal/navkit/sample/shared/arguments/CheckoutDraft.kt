package io.thernal.navkit.sample.shared.arguments

/** Small and serializable-shaped: an argument is an identifier or a draft, never a repository. */
data class CheckoutDraft(
    val amount: String = "",
    val address: String = "",
    val method: String = "",
)
