package io.thernal.navkit.navigation.impl.domain

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
    fun dispatchesCustomSchemeAndQueryToOwningHandler(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()))

            val result = dispatcher.dispatch("navkit://booking?id=42", DeepLinkSource.EXTERNAL_LINK)

            assertEquals(DeepLinkOutcome.Navigate(listOf(BookingRoute("42"))), result)
        }
    }

    @Test
    fun dispatchesTheWebFormOfTheSameLinkToTheSameHandler(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()))

            val result = dispatcher.dispatch(
                "https://example.com/booking?id=42",
                DeepLinkSource.EXTERNAL_LINK,
            )

            assertEquals(DeepLinkOutcome.Navigate(listOf(BookingRoute("42"))), result)
        }
    }

    @Test
    fun anUnclaimedPageIsNotFound(): TestResult {
        return runTest {
            val dispatcher = DeepLinkDispatcherImpl(setOf(BookingHandler()))

            val result = dispatcher.dispatch("navkit://profile", DeepLinkSource.EXTERNAL_LINK)

            assertEquals(DeepLinkOutcome.NotFound, result)
        }
    }

    @Test
    fun twoHandlersClaimingOnePageFailAtConstruction() {
        assertFailsWith<IllegalStateException> {
            DeepLinkDispatcherImpl(setOf(BookingHandler(), RivalBookingHandler()))
        }
    }
}
