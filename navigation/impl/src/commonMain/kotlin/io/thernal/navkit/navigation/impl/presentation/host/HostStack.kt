package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * The erasure boundary, in one place and under one name. A host is generic in its route type; the
 * core it delegates to is not, deliberately — one `LocalNavigator` serves screens that know nothing
 * of each other's types, and an app-wide guard judges every host's stack.
 *
 * **An assumption, not a proof.** It holds for anything that only reorders, drops or re-adds what
 * the host already had, which is every navigator command. It does *not* hold for a guard that
 * inserts a route of its own, and erasure lets that through — the failure surfaces as Navigation3's
 * `IllegalStateException("Unknown screen …")`. Tracked in `docs/todos/arguments.md` §3.
 */
internal fun <R : Route> ImmutableList<Route>.asHostStack(): ImmutableList<R> {
    @Suppress("UNCHECKED_CAST")
    return this as ImmutableList<R>
}
