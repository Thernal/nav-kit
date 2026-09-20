package io.thernal.navkit.navigation.impl.presentation.scene.bottomsheet

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Swallows every pointer event: a closing sheet is still on screen once its routes are gone. */
internal fun Modifier.ignorePointerInput(isIgnored: Boolean): Modifier {
    if (!isIgnored) {
        return this
    }
    return pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { change ->
                    change.consume()
                }
            }
        }
    }
}
