package io.thernal.navkit.sample.shared.guards

import io.thernal.navkit.navigation.api.presentation.model.Route

/** The same idea for the second guard: a route that may only be seen behind a PIN. */
interface PinProtected : Route
