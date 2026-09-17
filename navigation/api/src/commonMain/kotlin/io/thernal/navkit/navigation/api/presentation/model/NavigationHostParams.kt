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
 * Controlled like a `TextField`: the caller owns [backStack] and is notified of every navigation
 * command through [onBackStackChange], which must be a plain setter — the host writes its own guard
 * corrections back through it. `NavigationHost` builds a
 * [io.thernal.navkit.navigation.api.presentation.navigator.Navigator] over the pair.
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
     * Guards that apply to this host's stack only, on top of the application-wide ones — a wizard's
     * internal rules, or a guard whose dependencies live in a scope the app graph cannot reach.
     */
    val guards: ImmutableList<NavigationGuard> = persistentListOf(),
)
