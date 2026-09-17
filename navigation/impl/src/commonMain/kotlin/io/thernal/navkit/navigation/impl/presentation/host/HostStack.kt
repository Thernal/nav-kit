package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * The erasure boundary, in one place and under one name: a host is generic in its route type, the
 * core it delegates to deliberately is not. **An assumption, not a proof** — it holds for every
 * navigator command, but not for a guard that inserts a route of its own. Tracked in
 * `docs/todos/arguments.md` §3.
 */
internal fun <R : Route> ImmutableList<Route>.asHostStack(): ImmutableList<R> {
    @Suppress("UNCHECKED_CAST")
    return this as ImmutableList<R>
}
