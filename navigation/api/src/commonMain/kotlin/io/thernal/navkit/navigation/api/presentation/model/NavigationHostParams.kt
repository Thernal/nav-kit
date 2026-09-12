package io.thernal.navkit.navigation.api.presentation.model

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.transition.NavTransitionScope
import io.thernal.navkit.navigation.api.presentation.transition.PredictiveNavTransitionScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Controlled like a `TextField`: the caller owns [backStack] (its own `StateFlow`-backed state,
 * typically) and is notified of every navigation command through [onBackStackChange].
 * `NavigationHost` builds a [io.thernal.navkit.navigation.api.presentation.navigator.Navigator]
 * over this pair and provides it to its own content, so the navigation module never owns state.
 */
@Immutable
data class NavigationHostParams<R : Route>(
    val backStack: ImmutableList<R>,
    val onBackStackChange: (ImmutableList<R>) -> Unit,
    val transitionSpec: NavTransitionScope<R>? = null,
    val popTransitionSpec: NavTransitionScope<R>? = null,
    val predictivePopTransitionSpec: PredictiveNavTransitionScope<R>? = null,
    val decorators: ImmutableList<NavEntryDecorator<R>> = persistentListOf(),
    val sceneStrategies: ImmutableList<SceneStrategy<R>> = persistentListOf(),
    /**
     * Guards that apply to this host's stack only, on top of the application-wide ones. For a
     * wizard's internal rules, or a guard whose dependencies live in a feature scope and so could
     * never be contributed to the app graph's multibinding: the caller already holds that scope,
     * and hands the guard over here the same way it hands over [decorators].
     */
    val guards: ImmutableList<NavigationGuard> = persistentListOf(),
)
