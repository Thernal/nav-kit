package io.thernal.navkit.sample.shared.sheets

import io.thernal.navkit.sample.shared.app.SampleRoute

sealed interface SheetsRoute : SampleRoute

/**
 * The routes this package registers with `bottomSheetEntry` instead of `navEntry`.
 *
 * The host never reads it; it is what `closeSheet()` is written against.
 */
sealed interface SheetStepRoute : SheetsRoute

data object PostRoute : SheetsRoute

data object ShareSheetRoute : SheetStepRoute

data object BasketRoute : SheetsRoute

data object PaymentMethodSheetRoute : SheetStepRoute

data object AddCardSheetRoute : SheetStepRoute

/** An ordinary route, so it carries what the step before it chose. */
data class ConfirmCardSheetRoute(val maskedNumber: String) : SheetStepRoute
