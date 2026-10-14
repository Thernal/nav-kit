package io.thernal.navkit.navigation.testing

import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestNavigatorTest {
    private data object Home : Route

    private data class Detail(val id: String) : Route

    @Test
    fun `commands change the stack as the app's navigator does`() {
        val navigator = TestNavigator(listOf(Home))

        navigator.push(Detail("1"))
        navigator.push(Detail("2"))
        assertEquals<List<Route>>(listOf(Home, Detail("1"), Detail("2")), navigator.stack)

        assertTrue(navigator.popBack())
        navigator.replace(Detail("3"))
        assertEquals<List<Route>>(listOf(Home, Detail("3")), navigator.stack)
    }

    @Test
    fun `the root cannot be popped`() {
        val navigator = TestNavigator(listOf(Home))

        assertFalse(navigator.popBack())
        assertEquals<List<Route>>(listOf(Home), navigator.stack)
    }
}
