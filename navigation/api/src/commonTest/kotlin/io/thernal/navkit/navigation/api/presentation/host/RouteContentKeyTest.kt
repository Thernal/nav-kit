package io.thernal.navkit.navigation.api.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RouteContentKeyTest {
    private sealed interface ProfileRoute : Route {
        data object Main : ProfileRoute
    }

    private sealed interface SettingsRoute : Route {
        data object Main : SettingsRoute
    }

    private data class OrderRoute(val id: String) : Route

    @Test
    fun `two data objects with one simple name get different keys`() {
        assertEquals(ProfileRoute.Main.toString(), SettingsRoute.Main.toString())
        assertNotEquals(routeContentKey(ProfileRoute.Main), routeContentKey(SettingsRoute.Main))
    }

    @Test
    fun `a data class route is keyed by its arguments too`() {
        assertNotEquals(routeContentKey(OrderRoute(id = "1")), routeContentKey(OrderRoute(id = "2")))
        assertEquals(routeContentKey(OrderRoute(id = "1")), routeContentKey(OrderRoute(id = "1")))
    }
}
