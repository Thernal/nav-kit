package io.thernal.navkit.sample.app

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Every destination the sample's root host can render.
 *
 * Open rather than sealed, because each example owns its own package and a sealed hierarchy may not
 * cross one. Where exhaustiveness earns its keep it is applied per flow instead — a checkout's
 * steps are a sealed interface inside the checkout package — which is also the granularity a guard
 * or an argument scope wants: `whileInStack { it is CheckoutRoute }`.
 */
interface SampleRoute : Route
