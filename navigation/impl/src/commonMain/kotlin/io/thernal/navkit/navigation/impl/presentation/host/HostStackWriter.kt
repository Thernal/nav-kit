package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * Every stack this host writes, kept until the owner hands it back — the owner answers a frame late,
 * so a command builds on [newestOr] rather than on what is on screen, or it undoes the command
 * before it (`popBack(2)` popped once). The same record tells an own write from an external one.
 *
 * Plain fields, not snapshot state: a write from inside a command would invalidate its reader.
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
     * Records the stack the owner handed in, and answers whether a hand other than this host's
     * changed it. The first stack is nobody's change; a handed-back write confirms itself and every
     * write before it.
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
