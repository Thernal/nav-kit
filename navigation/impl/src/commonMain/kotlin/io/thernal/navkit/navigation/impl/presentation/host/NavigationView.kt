package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import io.thernal.navkit.navigation.api.presentation.back.LocalBackDispatcher
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuardRunner
import io.thernal.navkit.navigation.api.presentation.log.NavigationEventSink
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import io.thernal.navkit.navigation.impl.presentation.scene.BottomSheetSceneStrategy
import io.thernal.navkit.navigation.impl.presentation.scene.ModalSceneStrategy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun <R : Route> NavigationView(
    params: NavigationHostParams<R>,
    guardRunner: NavigationGuardRunner,
    backDispatcher: BackDispatcher,
    events: NavigationEventSink,
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<R>.() -> Unit,
) {
    if (LocalInspectionMode.current) {
        return
    }
    val currentBackStack by rememberUpdatedState(params.backStack)
    val currentOnBackStackChange by rememberUpdatedState(params.onBackStackChange)

    // Built once and remembered so the navigator identity stays stable across recompositions —
    // every read/write goes through the `rememberUpdatedState` closures above, never captured here.
    val navigator = remember {
        BackStackNavigator(
            buildBackStack = { builder ->
                @Suppress("UNCHECKED_CAST")
                val mutable = currentBackStack.toMutableList() as MutableList<Route>
                mutable.builder()
                @Suppress("UNCHECKED_CAST")
                currentOnBackStackChange(mutable.toImmutableList() as ImmutableList<R>)
            },
            resolveCanPop = { currentBackStack.size > 1 },
            guardRunner = guardRunner,
            events = events,
        )
    }

    val provider = entryProvider(builder = entries)
    val pushSpec = remember(params.transitionSpec) {
        params.transitionSpec ?: NavAnimations.push()
    }
    val popSpec = remember(params.popTransitionSpec) {
        params.popTransitionSpec ?: NavAnimations.pop()
    }
    val predictiveSpec = remember(params.predictivePopTransitionSpec) {
        params.predictivePopTransitionSpec ?: NavAnimations.predictivePop()
    }
    val builtInDecorators = persistentListOf(
        rememberSaveableStateHolderNavEntryDecorator<R>(),
        rememberViewModelStoreNavEntryDecorator<R>(),
    )
    // Caller strategies come first so an app can claim a route before the built-in overlays see it;
    // SinglePaneSceneStrategy is last because it claims everything.
    val builtInScenes = remember {
        persistentListOf<SceneStrategy<R>>(BottomSheetSceneStrategy(), ModalSceneStrategy())
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalBackDispatcher provides backDispatcher,
    ) {
        NavDisplay(
            modifier = modifier,
            backStack = params.backStack,
            onBack = {
                if (!backDispatcher.dispatch()) {
                    navigator.popBack()
                }
            },
            sceneStrategies = params.sceneStrategies + builtInScenes + SinglePaneSceneStrategy(),
            transitionSpec = pushSpec,
            popTransitionSpec = popSpec,
            predictivePopTransitionSpec = predictiveSpec,
            entryDecorators = builtInDecorators + params.decorators,
            entryProvider = provider,
        )
    }
}
