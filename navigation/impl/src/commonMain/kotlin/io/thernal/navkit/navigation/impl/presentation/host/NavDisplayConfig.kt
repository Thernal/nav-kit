package io.thernal.navkit.navigation.impl.presentation.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import io.thernal.navkit.navigation.api.presentation.model.NavigationHostParams
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.transition.NavTransitionScope
import io.thernal.navkit.navigation.api.presentation.transition.PredictiveNavTransitionScope
import io.thernal.navkit.navigation.impl.presentation.scene.BottomSheetSceneStrategy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/** Everything `NavDisplay` needs that is not the stack itself, with the caller's defaults filled. */
internal class NavDisplayConfig<R : Route>(
    val entryProvider: (R) -> NavEntry<R>,
    val transitionSpec: NavTransitionScope<R>,
    val popTransitionSpec: NavTransitionScope<R>,
    val predictivePopTransitionSpec: PredictiveNavTransitionScope<R>,
    val decorators: ImmutableList<NavEntryDecorator<R>>,
    val sceneStrategies: ImmutableList<SceneStrategy<R>>,
)

/**
 * Memoizes the configuration so a recomposition that only moved the stack does not rebuild it —
 * Navigation3's `entryProvider` runs the whole builder on every call.
 */
@Composable
internal fun <R : Route> rememberNavDisplayConfig(
    params: NavigationHostParams<R>,
    entries: EntryProviderScope<R>.() -> Unit,
): NavDisplayConfig<R> {
    val provider = remember(entries) {
        entryProvider(builder = entries)
    }
    val pushSpec = remember(params.transitionSpec) {
        params.transitionSpec ?: NavAnimations.push()
    }
    val popSpec = remember(params.popTransitionSpec) {
        params.popTransitionSpec ?: NavAnimations.pop()
    }
    val predictiveSpec = remember(params.predictivePopTransitionSpec) {
        params.predictivePopTransitionSpec ?: NavAnimations.predictivePop()
    }

    val saveableStateDecorator = rememberSaveableStateHolderNavEntryDecorator<R>()
    val viewModelStoreDecorator = rememberViewModelStoreNavEntryDecorator<R>()
    val decorators = remember(
        key1 = saveableStateDecorator,
        key2 = viewModelStoreDecorator,
        key3 = params.decorators,
    ) {
        persistentListOf(saveableStateDecorator, viewModelStoreDecorator)
            .addingAll(params.decorators)
    }

    // Caller strategies come first so an app can claim a route before the built-in overlay sees it;
    // SinglePaneSceneStrategy is last because it claims everything.
    val sceneStrategies = remember(params.sceneStrategies) {
        params.sceneStrategies
            .toPersistentList()
            .adding(BottomSheetSceneStrategy())
            .adding(SinglePaneSceneStrategy())
    }

    return remember(
        provider,
        pushSpec,
        popSpec,
        predictiveSpec,
        decorators,
        sceneStrategies,
    ) {
        NavDisplayConfig(
            entryProvider = provider,
            transitionSpec = pushSpec,
            popTransitionSpec = popSpec,
            predictivePopTransitionSpec = predictiveSpec,
            decorators = decorators,
            sceneStrategies = sceneStrategies,
        )
    }
}
