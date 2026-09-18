package io.thernal.navkit.sample.arguments

import io.thernal.navkit.navigation.api.presentation.argument.argumentKey
import io.thernal.navkit.sample.app.SampleRoute

sealed interface ArgumentsRoute : SampleRoute

data object GreetingSetupRoute : ArgumentsRoute

data object GreetingReaderRoute : ArgumentsRoute

data object CheckoutStartRoute : ArgumentsRoute

/**
 * The flow's own routes, sealed apart from the rest.
 *
 * This is the type the argument scopes itself to — `whileInStack { it is CheckoutStepRoute }` —
 * which is what makes back, `popBackTo`, a guard rewrite and a deep link all come out right without
 * any of them being handled separately.
 */
sealed interface CheckoutStepRoute : ArgumentsRoute

data object CheckoutAmountRoute : CheckoutStepRoute

data object CheckoutAddressRoute : CheckoutStepRoute

data object CheckoutPaymentRoute : CheckoutStepRoute

data object CheckoutSummaryRoute : CheckoutStepRoute

/** Small and serializable-shaped: an argument is an identifier or a draft, never a repository. */
data class CheckoutDraft(
    val amount: String = "",
    val address: String = "",
    val method: String = "",
)

val GreetingName = argumentKey<String>("arguments.greeting_name")

val CheckoutDraftKey = argumentKey<CheckoutDraft>("arguments.checkout_draft")
