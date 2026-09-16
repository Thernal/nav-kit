package io.thernal.navkit.sample.results

import io.thernal.navkit.navigation.api.presentation.result.resultKey
import io.thernal.navkit.sample.app.SampleRoute

sealed interface ResultsRoute : SampleRoute

data object PickerHomeRoute : ResultsRoute

data object PickerRoute : ResultsRoute

data object ReviewHomeRoute : ResultsRoute

data class ReviewStepRoute(val step: Int) : ResultsRoute

/** What the review flow hands back. A result is a value, not a signal. */
data class ReviewDecision(
    val isApproved: Boolean,
    val note: String,
)

/**
 * Declared once, next to the routes of the feature that produces them, and used by both sides.
 *
 * That is the whole point of a typed key: a producer writing an `Int` and a consumer asking for a
 * `String` used to get `null`, silently. Now they cannot disagree without the compiler saying so,
 * and if two features ever declare the same name with different types, `consume` throws instead of
 * quietly answering nothing.
 */
val SelectedColour = resultKey<String>("results.selected_colour")

val ReviewOutcome = resultKey<ReviewDecision>("results.review_outcome")
