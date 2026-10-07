package io.thernal.navkit.sample.shared.arguments

import io.thernal.navkit.navigation.api.presentation.argument.ArgumentScope
import io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack

/** Alive while any step of the flow is on the stack — the start screen is not a step. */
internal val inCheckout: ArgumentScope = whileInStack { route -> route is CheckoutStepRoute }

internal fun NavigationArguments.draft(): CheckoutDraft {
    return get(CheckoutDraftKey) ?: CheckoutDraft()
}

/**
 * Re-putting under the same key replaces the value. [change] is applied to the draft as it is now,
 * not as the screen last saw it, so two steps never write over each other's fields.
 */
internal fun NavigationArguments.update(change: (CheckoutDraft) -> CheckoutDraft) {
    put(
        key = CheckoutDraftKey,
        value = change(draft()),
        scope = inCheckout,
    )
}
