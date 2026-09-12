package io.thernal.navkit.navigation.api.presentation.model

/**
 * A route that does not outlive the process. The host drops these from a stack it restores, before
 * anything is rendered or guarded.
 *
 * The case that needs it is a guard's placeholder: routes survive process death and an in-flight
 * coroutine does not, so a `Loading` route shown while a deferral runs would come back with nothing
 * left to resolve it — a screen that never finishes. Marking it here makes that impossible rather
 * than making it somebody's job to remember.
 */
interface TransientRoute : Route
