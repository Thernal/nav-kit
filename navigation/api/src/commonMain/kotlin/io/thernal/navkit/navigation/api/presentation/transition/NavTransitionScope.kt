package io.thernal.navkit.navigation.api.presentation.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.navigation3.scene.Scene

typealias NavTransitionScope<R> = AnimatedContentTransitionScope<Scene<R>>.() -> ContentTransform
typealias PredictiveNavTransitionScope<R> = AnimatedContentTransitionScope<Scene<R>>.(Int) -> ContentTransform
