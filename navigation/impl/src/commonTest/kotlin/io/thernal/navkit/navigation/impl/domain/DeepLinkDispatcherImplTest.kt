package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.domain.DeepLinkBase
import io.thernal.navkit.navigation.api.domain.DeepLinkRequest
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkHandler
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.domain.deeplink.DeepLinkDispatcherImpl
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeepLinkDispatcherImplTest {
    private data class BookingRoute(val id: String) : Route

    private val bases = setOf(DeepLinkBase("navkit://"), DeepLinkBase("https://example.com"))

    private class BookingHandler : DeepLinkHandler {
        override val pages = setOf("booking")

        override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
            val id = request.deepLink.query("id") ?: return DeepLinkOutcome.Rejected("missing id")
            return DeepLinkOutcome.Navigate(listOf(BookingRoute(id)))
        }
    }

    /** Claims the same page as [BookingHandler], which the dispatcher must refuse. */
    private class RivalBookingHandler : DeepLinkHandler {
        override val pages = setOf("booking")

        override suspend fun resolve(request: DeepLinkRequest): DeepLinkOutcome {
            return DeepLinkOutcome.NotFound
        }
    }

    // The block body returns the TestResult rather than discarding it: on a JS target that value
    // is the only thing that keeps the runner waiting for the coroutine.
    @Test
    fun `dispatches custom scheme and query to owning handler`(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()), bases)

            val result = dispatcher.dispatch("navkit://booking?id=42", DeepLinkSource.EXTERNAL_LINK)

            assertEquals(DeepLinkOutcome.Navigate(listOf(BookingRoute("42"))), result)
        }
    }

    @Test
    fun `dispatches the web form of the same link to the same handler`(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()), bases)

            val result = dispatcher.dispatch(
                "https://example.com/booking?id=42",
                DeepLinkSource.EXTERNAL_LINK,
            )

            assertEquals(DeepLinkOutcome.Navigate(listOf(BookingRoute("42"))), result)
        }
    }

    @Test
    fun `an unclaimed page is not found`(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()), bases)

            val result = dispatcher.dispatch("navkit://profile", DeepLinkSource.EXTERNAL_LINK)

            assertEquals(DeepLinkOutcome.NotFound, result)
        }
    }

    @Test
    fun `a link on a domain the app does not own is not found`(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()), bases)

            val result = dispatcher.dispatch(
                "https://elsewhere.example/booking?id=42",
                DeepLinkSource.EXTERNAL_LINK,
            )

            assertEquals(DeepLinkOutcome.NotFound, result)
        }
    }

    @Test
    fun `handlers with no base fail at construction`() {
        assertFailsWith<IllegalArgumentException> {
            DeepLinkDispatcherImpl(setOf(BookingHandler()), emptySet())
        }
    }

    @Test
    fun `two handlers claiming one page fail at construction`() {
        assertFailsWith<IllegalStateException> {
            DeepLinkDispatcherImpl(setOf(BookingHandler(), RivalBookingHandler()), bases)
        }
    }
}
