package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.domain.DeepLinkBase
import io.thernal.navkit.navigation.api.domain.buildDeepLinkUri
import io.thernal.navkit.navigation.impl.domain.deeplink.parseDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeepLinkParserTest {
    private val appScheme = DeepLinkBase("navkit://")
    private val web = DeepLinkBase("https://example.com")
    private val webApp = DeepLinkBase("https://example.com/app")
    private val bases = setOf(appScheme, web)

    @Test
    fun anAppSchemeBaseLeavesTheHostAsTheFirstPage() {
        val link = parseDeepLink("navkit://booking/42", bases)

        assertEquals(listOf("booking", "42"), link?.pathSegments)
        assertEquals("booking", link?.page)
        assertEquals("navkit", link?.scheme)
        assertEquals(appScheme, link?.base)
    }

    @Test
    fun aWebBaseRemovesItsHost() {
        val link = parseDeepLink("https://example.com/booking/42", bases)

        assertEquals(listOf("booking", "42"), link?.pathSegments)
        assertEquals("booking", link?.page)
        assertEquals("example.com", link?.host)
        assertEquals(web, link?.base)
    }

    @Test
    fun aWebBaseRemovesItsPathToo() {
        val link = parseDeepLink("https://example.com/app/booking/42", setOf(webApp))

        assertEquals(listOf("booking", "42"), link?.pathSegments)
    }

    @Test
    fun theMostSpecificMatchingBaseWins() {
        val link = parseDeepLink("https://example.com/app/booking", setOf(web, webApp))

        assertEquals(webApp, link?.base)
        assertEquals("booking", link?.page)
    }

    @Test
    fun aBasePathMatchesWholeSegmentsOnly() {
        assertNull(parseDeepLink("https://example.com/apple/booking", setOf(webApp)))
    }

    @Test
    fun aLinkThatStartsWithNoRegisteredBaseIsRejected() {
        assertNull(parseDeepLink("https://elsewhere.example/booking/42", bases))
        assertNull(parseDeepLink("otherapp://booking/42", bases))
        assertNull(parseDeepLink("navkit://booking/42", emptySet()))
    }

    @Test
    fun theSchemeAndHostCompareCaseInsensitively() {
        val link = parseDeepLink("HTTPS://Example.COM/booking", bases)

        assertEquals(web, link?.base)
        assertEquals("booking", link?.page)
    }

    @Test
    fun queryValuesAreDecodedAndGroupedByKey() {
        val link = parseDeepLink("navkit://search?q=bar%20table&tag=a&tag=b", bases)

        assertEquals("bar table", link?.query("q"))
        assertEquals(listOf("a", "b"), link?.query?.get("tag"))
    }

    @Test
    fun pathSegmentsAreDecoded() {
        val link = parseDeepLink("https://example.com/venue/bar%20table", bases)

        assertEquals(listOf("venue", "bar table"), link?.pathSegments)
    }

    @Test
    fun aLinkWithNoPageIsRejected() {
        assertNull(parseDeepLink("https://example.com", bases))
        assertNull(parseDeepLink("https://example.com/app", setOf(webApp)))
        assertNull(parseDeepLink("   ", bases))
    }

    @Test
    fun aBuiltLinkIsReadBackAsTheSamePage() {
        listOf(appScheme, web, webApp).forEach { base ->
            val built = buildDeepLinkUri(base = base, page = "profile", query = mapOf("id" to "42 a"))

            val link = parseDeepLink(built, setOf(appScheme, web, webApp))

            assertEquals(base, link?.base, built)
            assertEquals(listOf("profile"), link?.pathSegments, built)
            assertEquals("42 a", link?.query("id"), built)
        }
    }

    @Test
    fun aBuiltLinkHasTheShapeOfItsBase() {
        assertEquals("navkit://profile?id=42", buildDeepLinkUri(appScheme, "profile", mapOf("id" to "42")))
        assertEquals("https://example.com/profile", buildDeepLinkUri(web, "profile"))
        assertEquals("https://example.com/app/profile", buildDeepLinkUri(webApp, "profile"))
    }

    @Test
    fun aBaseIsNormalizedSoEquivalentFormsAreEqual() {
        assertEquals(DeepLinkBase("https://example.com"), DeepLinkBase("HTTPS://Example.com/"))
        assertEquals("navkit://", DeepLinkBase("NavKit://").uri)
    }

    @Test
    fun aMalformedBaseIsRejected() {
        assertFailsWith<IllegalArgumentException> { DeepLinkBase("navkit") }
        assertFailsWith<IllegalArgumentException> { DeepLinkBase("https://example.com?tab=1") }
        assertFailsWith<IllegalArgumentException> { DeepLinkBase("https://") }
        assertFailsWith<IllegalArgumentException> { DeepLinkBase("navkit:///app") }
    }
}
