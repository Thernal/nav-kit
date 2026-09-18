package io.thernal.navkit.sample.backoverride

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.navigation.api.presentation.guard.GuardVerdict
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * A rule about *leaving* a screen, which is the one kind of rule that needs the transition.
 *
 * Almost every guard is about a destination and should extend `RouteGuard`. This one cannot: "the
 * editor may not be left with unsaved work" is a statement about the difference between the stack
 * that was and the stack being proposed, and a destination-only guard cannot express it at all.
 *
 * It is phrased narrowly on purpose — about this screen's own route, never about movement in
 * general. `old` does not advance during the fold, so a rule broad enough to reject any difference
 * between the two stacks would undo every rewriting guard, which would rewrite again, and the pair
 * would fail at the round limit instead of settling.
 *
 * Because it guards the transition rather than the destination, it covers every way out at once:
 * the system gesture, the in-app Back button, and a `popBackTo` that jumps past the dispatcher.
 */
class UnsavedWorkGuard(private val drafts: ArticleDraftStore) : NavigationGuard {
    private val reason: BlockReason = UnsavedWork

    override fun evaluate(
        old: ImmutableList<Route>,
        new: ImmutableList<Route>,
    ): GuardVerdict {
        val wasEditing = old.any { route -> route is ArticleEditorRoute }
        val isStillEditing = new.any { route -> route is ArticleEditorRoute }
        if (wasEditing && !isStillEditing && drafts.hasUnsavedChanges) {
            // Returning the previous stack *is* the refusal. There is no separate "blocked" verdict
            // because there does not need to be: the answer is always the stack that may exist.
            return GuardVerdict.Resolved(stack = old, reason = reason)
        }
        return GuardVerdict.Resolved(new)
    }
}
