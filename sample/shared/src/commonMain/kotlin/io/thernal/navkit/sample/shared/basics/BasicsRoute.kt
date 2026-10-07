package io.thernal.navkit.sample.basics

import io.thernal.navkit.sample.app.SampleRoute

/**
 * The routes of both navigation examples.
 *
 * Sealed within this package, which is the granularity that earns it: a `when` over the wizard's
 * steps is exhaustive, and a guard or an argument scope can name the whole flow with `it is
 * BasicsRoute` rather than listing screens.
 */
sealed interface BasicsRoute : SampleRoute

data object BasicsHomeRoute : BasicsRoute

data class BasicsDetailRoute(val id: String) : BasicsRoute

data object WizardRoute : BasicsRoute

data class WizardStepRoute(val step: Int) : BasicsRoute

data object WizardDoneRoute : BasicsRoute
