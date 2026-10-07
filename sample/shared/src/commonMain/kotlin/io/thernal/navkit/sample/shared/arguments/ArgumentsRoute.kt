package io.thernal.navkit.sample.shared.arguments

import io.thernal.navkit.sample.shared.app.SampleRoute

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
sealed interface CheckoutStepRoute : ArgumentsRoute {
    companion object {
        /** The steps the header counts, the summary included. */
        const val COUNT = 4
    }
}

data object CheckoutAmountRoute : CheckoutStepRoute

data object CheckoutAddressRoute : CheckoutStepRoute

data object CheckoutPaymentRoute : CheckoutStepRoute

data object CheckoutSummaryRoute : CheckoutStepRoute
