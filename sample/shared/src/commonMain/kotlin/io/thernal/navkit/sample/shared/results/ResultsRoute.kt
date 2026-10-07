package io.thernal.navkit.sample.shared.results

import io.thernal.navkit.sample.shared.app.SampleRoute

sealed interface ResultsRoute : SampleRoute

data object PickerHomeRoute : ResultsRoute

data object PickerRoute : ResultsRoute

data object ReviewHomeRoute : ResultsRoute

data class ReviewStepRoute(val step: Int) : ResultsRoute
