package io.thernal.navkit.navigation.impl.presentation.scene.bottomsheet

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.thernal.navkit.navigation.api.presentation.host.BOTTOM_SHEET_METADATA_KEY
import io.thernal.navkit.navigation.api.presentation.host.BottomSheetContainer
import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * Claims the run of consecutive bottom-sheet entries at the top of the stack.
 *
 * The scene is kept across a step change rather than rebuilt — Navigation3 keys an overlay's
 * composition on the instance, so a new one per step would reopen the sheet on every push.
 */
class BottomSheetSceneStrategy<R : Route>(
    private val container: BottomSheetContainer? = null,
) : SceneStrategy<R> {
    // At most one: a run is the sheet entries on top of the stack.
    private var open: BottomSheetScene<R>? = null

    override fun SceneStrategyScope<R>.calculateScene(entries: List<NavEntry<R>>): Scene<R>? {
        if (entries.lastOrNull()?.metadata?.get(BOTTOM_SHEET_METADATA_KEY) != true) {
            return null
        }
        val steps = entries.takeLastWhile { entry ->
            entry.metadata[BOTTOM_SHEET_METADATA_KEY] == true
        }
        val below = entries.dropLast(steps.size)
        val runKey = steps.first().contentKey

        // A sheet reopened while the last is still animating out needs a scene of its own.
        val current = open?.takeIf { scene -> scene.key == runKey && !scene.isClosing }
        if (current != null) {
            current.moveTo(steps = steps, below = below)
            return current
        }
        val scene = BottomSheetScene(
            key = runKey,
            initialSteps = steps,
            initialBelow = below,
            container = container,
            onClosed = { closed ->
                // Unless a newer sheet claimed the slot while this one was animating out.
                if (open === closed) {
                    open = null
                }
            },
            onBack = onBack,
        )
        open = scene
        return scene
    }
}
