package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.Navigator
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * One navigator per host, built once so its identity stays stable across recompositions — every
 * read and write goes through the [rememberUpdatedState] boxes below rather than through anything
 * captured at construction. A navigator that captured the first composition's stack would keep
 * writing over it forever.
 */
@Composable
internal fun <R : Route> rememberHostNavigator(
    backStack: ImmutableList<R>,
    onBackStackChange: (ImmutableList<R>) -> Unit,
    guardRunner: NavigationGuardRunner,
    backDispatcher: BackDispatcher,
    events: NavigationEventSink,
): Navigator {
    val currentBackStack by rememberUpdatedState(backStack)
    val currentOnBackStackChange by rememberUpdatedState(onBackStackChange)
    val currentGuardRunner by rememberUpdatedState(guardRunner)

    return remember {
        BackStackNavigator(
            buildBackStack = { builder ->
                // A fresh `MutableList<Route>` rather than a cast of the host's own list: the
                // builder may add any `Route`, and handing it a list typed `R` would be a lie that
                // outlives this call. `ImmutableList` is covariant, so the copy needs no cast.
                val mutable: MutableList<Route> = currentBackStack.toMutableList()
                mutable.builder()
                currentOnBackStackChange(mutable.toImmutableList().asHostStack())
            },
            resolveCanPop = { currentBackStack.size > 1 },
            resolveGuardRunner = { currentGuardRunner },
            backDispatcher = backDispatcher,
            events = events,
        )
    }
}
