package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * Every stack this host writes, kept until the owner hands it back.
 *
 * The owner answers late — a `StateFlow` reaches the host a frame after `onBackStackChange` returned
 * — so for that frame the stack on screen is not the one last written, and a command built on the
 * screen would undo the command before it (`popBack(2)` popped once). Commands build on [newestOr].
 *
 * The same record tells a stack this host wrote apart from one that arrived from elsewhere, which is
 * what a waiting deferral needs in order to be walked away from. See [acknowledge].
 *
 * Plain fields, not snapshot state: nothing here decides what composes, and a snapshot write from
 * inside a command would invalidate the composition reading it.
 */
internal class HostStackWriter<R : Route>(private val deliver: (ImmutableList<R>) -> Unit) {
    private var handed: ImmutableList<R>? = null

    /** Oldest first. The owner may never hand some of them back — a conflated flow skips them. */
    private val unconfirmed = ArrayDeque<ImmutableList<R>>()

    fun write(stack: ImmutableList<R>) {
        unconfirmed.addLast(stack)
        deliver(stack)
    }

    /** The stack a command starts from: the newest write not yet handed back, or else [rendered]. */
    fun newestOr(rendered: ImmutableList<R>): ImmutableList<R> {
        return unconfirmed.lastOrNull() ?: rendered
    }

    /**
     * Records the stack the owner handed in, and answers whether it was changed by a hand other than
     * this host's. The first stack is nobody's change; a handed-back write confirms itself and every
     * write before it; anything else is external, and the waiting writes are dropped.
     */
    fun acknowledge(stack: ImmutableList<R>): Boolean {
        val previous = handed
        if (stack == previous) {
            return false
        }
        handed = stack
        val ownIndex = unconfirmed.indexOf(stack)
        if (ownIndex == -1) {
            unconfirmed.clear()
            return previous != null
        }
        repeat(ownIndex + 1) { unconfirmed.removeFirstOrNull() }
        return false
    }
}
