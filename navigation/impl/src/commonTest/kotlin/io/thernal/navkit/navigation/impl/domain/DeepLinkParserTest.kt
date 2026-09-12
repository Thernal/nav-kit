package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.impl.domain.deeplink.parseDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParserTest {
    @Test
    fun aCustomSchemeHostIsTheFirstPage() {
        val link = parseDeepLink("navkit://booking/42")

        assertEquals(listOf("booking", "42"), link?.pathSegments)
        assertEquals("booking", link?.page)
        assertEquals("navkit", link?.scheme)
    }

    @Test
    fun aWebHostIsADomainAndNotAPage() {
        val link = parseDeepLink("https://example.com/booking/42")

        assertEquals(listOf("booking", "42"), link?.pathSegments)
        assertEquals("booking", link?.page)
        assertEquals("example.com", link?.host)
    }

    @Test
    fun queryValuesAreDecodedAndGroupedByKey() {
        val link = parseDeepLink("navkit://search?q=bar%20table&tag=a&tag=b")

        assertEquals("bar table", link?.query("q"))
        assertEquals(listOf("a", "b"), link?.query?.get("tag"))
    }

    @Test
    fun pathSegmentsAreDecoded() {
        val link = parseDeepLink("https://example.com/venue/bar%20table")

        assertEquals(listOf("venue", "bar table"), link?.pathSegments)
    }

    @Test
    fun aLinkWithNoPageIsRejected() {
        assertNull(parseDeepLink("https://example.com"))
        assertNull(parseDeepLink("   "))
    }
}
