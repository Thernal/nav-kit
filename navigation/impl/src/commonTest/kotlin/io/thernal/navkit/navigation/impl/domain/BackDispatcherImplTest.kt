package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackDispatcherImplTest {
    @Test
    fun `dispatch returns false with no callbacks`() {
        val dispatcher = BackDispatcherImpl()

        assertFalse(dispatcher.dispatch())
        assertFalse(dispatcher.hasCallbacks())
    }

    @Test
    fun `the most recently registered callback wins`() {
        val dispatcher = BackDispatcherImpl()
        val handled = mutableListOf<String>()
        dispatcher.register(
            BackCallback {
                handled += "first"
                true
            },
        )
        dispatcher.register(
            BackCallback {
                handled += "second"
                true
            },
        )

        assertTrue(dispatcher.dispatch())
        assertEquals(listOf("second"), handled)
    }

    @Test
    fun `a callback that declines falls through to the next one`() {
        val dispatcher = BackDispatcherImpl()
        val handled = mutableListOf<String>()
        dispatcher.register(
            BackCallback {
                handled += "first"
                true
            },
        )
        dispatcher.register(
            BackCallback {
                handled += "second"
                false
            },
        )

        assertTrue(dispatcher.dispatch())
        assertEquals(listOf("second", "first"), handled)
    }

    @Test
    fun `closing the registration stops interception`() {
        val dispatcher = BackDispatcherImpl()
        val registration = dispatcher.register(BackCallback { true })

        registration.close()

        assertFalse(dispatcher.hasCallbacks())
        assertFalse(dispatcher.dispatch())
    }

    @Test
    fun `a nested dispatch consumes nothing`() {
        // A callback that lets back through by calling the navigator — which consults this
        // dispatcher first — would otherwise be dispatched to again, forever.
        val dispatcher = BackDispatcherImpl()
        var wasNestedConsumed: Boolean? = null
        dispatcher.register(
            BackCallback {
                wasNestedConsumed = dispatcher.dispatch()
                true
            },
        )

        assertTrue(dispatcher.dispatch())

        assertEquals(false, wasNestedConsumed)
    }
}
