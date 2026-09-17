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
 * The two navigators a host builds over one stack. [screens] goes to the content: it abandons a
 * waiting deferral when it moves the stack, and submits the ones it meets. [deferral] does neither —
 * a running deferral's own push is part of the wait.
 */
internal class HostNavigators(
    val screens: Navigator,
    val deferral: Navigator,
)

/**
 * Built once, so every read goes through the [rememberUpdatedState] boxes below and every write
 * through [writer]: a navigator that captured the first composition's stack would write over it
 * forever.
 */
@Composable
internal fun <R : Route> rememberHostNavigators(
    backStack: ImmutableList<R>,
    writer: HostStackWriter<R>,
    deferrals: HostDeferrals,
    guardRunner: NavigationGuardRunner,
    backDispatcher: BackDispatcher,
    events: NavigationEventSink,
): HostNavigators {
    val currentBackStack by rememberUpdatedState(backStack)
    val currentGuardRunner by rememberUpdatedState(guardRunner)

    return remember(key1 = writer, key2 = deferrals) {
        val buildBackStack: (MutableList<Route>.() -> Unit) -> Unit = { builder ->
            // A fresh `MutableList<Route>`, not a cast of the host's own list: the builder may add
            // any `Route`, and a list typed `R` would be a lie that outlives this call.
            val mutable: MutableList<Route> = writer.newestOr(currentBackStack).toMutableList()
            mutable.builder()
            writer.write(mutable.toImmutableList().asHostStack())
        }
        val resolveCanPop = { writer.newestOr(currentBackStack).size > 1 }
        val resolveGuardRunner = { currentGuardRunner }
        HostNavigators(
            screens = BackStackNavigator(
                buildBackStack = buildBackStack,
                resolveCanPop = resolveCanPop,
                resolveGuardRunner = resolveGuardRunner,
                backDispatcher = backDispatcher,
                events = events,
                onDeferred = deferrals::submit,
                onMoved = deferrals::abandon,
            ),
            deferral = BackStackNavigator(
                buildBackStack = buildBackStack,
                resolveCanPop = resolveCanPop,
                resolveGuardRunner = resolveGuardRunner,
                backDispatcher = backDispatcher,
                events = events,
            ),
        )
    }
}
