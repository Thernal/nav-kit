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
 * The two navigators a host builds over one stack. [screens] is what the host provides to its
 * content: a command that moves the stack walks away from a waiting deferral, and a command that is
 * deferred hands its deferral to the host. [deferral] is what a deferral itself is given, and does
 * neither — its placeholder push is part of the wait, not a way out of it.
 */
internal class HostNavigators(
    val screens: Navigator,
    val deferral: Navigator,
)

/**
 * Built once so their identity stays stable across recompositions — every read goes through the
 * [rememberUpdatedState] boxes below and every write through [writer], rather than through anything
 * captured at construction. A navigator that captured the first composition's stack would keep
 * writing over it forever.
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
            // A fresh `MutableList<Route>` rather than a cast of the host's own list: the builder
            // may add any `Route`, and handing it a list typed `R` would be a lie that outlives this
            // call. `ImmutableList` is covariant, so the copy needs no cast.
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
