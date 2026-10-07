package io.thernal.navkit.sample.sheets

import io.thernal.navkit.navigation.api.presentation.navigator.Navigator

/**
 * Closes the sheet rather than one step of it, however many steps were opened.
 *
 * The surface does the same through the `dismiss` it is handed; inside a step, where the routes are
 * this package's own, the predicate says it plainly.
 */
internal fun Navigator.closeSheet() {
    popBackTo { route -> route !is SheetStepRoute }
}
