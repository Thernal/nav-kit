package io.thernal.navkit.navigation.api.presentation.model

/**
 * A route that does not outlive the process; the host drops these from a restored stack. The case
 * that needs it is a guard's placeholder, which survives process death while the coroutine that was
 * going to resolve it does not.
 */
interface TransientRoute : Route
