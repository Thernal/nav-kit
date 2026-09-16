package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.collections.immutable.ImmutableList

/**
 * The erasure boundary, in one place and under one name.
 *
 * A host is generic in its own route type; the navigation core it delegates to — `Navigator`,
 * `NavigationGuardRunner` — is not, and deliberately so: `LocalNavigator` provides one navigator to
 * screens that know nothing of each other's route types, and an application-wide guard has to judge
 * every host's stack. So a stack leaves this host typed and comes back as `ImmutableList<Route>`.
 *
 * **This is an assumption, not a proof.** It holds for everything that only reorders, drops or
 * re-adds what the host already had, which is every navigator command. It does *not* hold for a
 * guard that inserts a route of its own — an application-wide sign-in guard rewriting a nested
 * host's stack, say. Erasure lets that cast through, and the failure surfaces later as
 * Navigation3's `IllegalStateException("Unknown screen …")` from the entry provider. That hole is
 * tracked in `docs/todos/arguments.md` §3; the decision it needs is about guard scope, and no
 * typing trick here can stand in for it.
 */
internal fun <R : Route> ImmutableList<Route>.asHostStack(): ImmutableList<R> {
    @Suppress("UNCHECKED_CAST")
    return this as ImmutableList<R>
}
