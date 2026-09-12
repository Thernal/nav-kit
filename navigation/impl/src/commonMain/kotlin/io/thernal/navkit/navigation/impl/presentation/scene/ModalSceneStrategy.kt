package io.thernal.navkit.navigation.impl.presentation.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import io.thernal.navkit.navigation.api.presentation.host.MODAL_METADATA_KEY
import io.thernal.navkit.navigation.api.presentation.model.Route

/** Claims a single modal entry at the top of the stack and draws it over the full window. */
class ModalSceneStrategy<R : Route> : SceneStrategy<R> {
    override fun SceneStrategyScope<R>.calculateScene(entries: List<NavEntry<R>>): Scene<R>? {
        val entry = entries.lastOrNull()?.takeIf { candidate ->
            candidate.metadata[MODAL_METADATA_KEY] == true
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        return ModalScene(
            key = entry.contentKey as R,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = entry,
        )
    }
}

private class ModalScene<R : Route>(
    override val key: R,
    override val previousEntries: List<NavEntry<R>>,
    override val overlaidEntries: List<NavEntry<R>>,
    private val entry: NavEntry<R>,
) : OverlayScene<R> {
    override val entries: List<NavEntry<R>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize()) { entry.Content() }
    }
}
