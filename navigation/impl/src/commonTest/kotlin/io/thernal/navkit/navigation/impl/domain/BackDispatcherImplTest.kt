package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackDispatcherImplTest {
    @Test
    fun dispatchReturnsFalseWithNoCallbacks() {
        val dispatcher = BackDispatcherImpl()

        assertFalse(dispatcher.dispatch())
        assertFalse(dispatcher.hasCallbacks())
    }

    @Test
    fun theMostRecentlyRegisteredCallbackWins() {
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
    fun aCallbackThatDeclinesFallsThroughToTheNextOne() {
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
    fun closingTheRegistrationStopsInterception() {
        val dispatcher = BackDispatcherImpl()
        val registration = dispatcher.register(BackCallback { true })

        registration.close()

        assertFalse(dispatcher.hasCallbacks())
        assertFalse(dispatcher.dispatch())
    }
}
