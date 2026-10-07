package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.result.ResultKey
import io.thernal.navkit.navigation.api.presentation.result.resultKey
import io.thernal.navkit.navigation.impl.domain.result.NavigationResultsImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigationResultsImplTest {
    private val selectedPhoto = resultKey<String>("selected_photo")
    private val count = resultKey<Int>("count")

    @Test
    fun `a result is read once and then gone`() {
        val results = NavigationResultsImpl()
        results.post(key = selectedPhoto, value = "photo-1")

        assertEquals("photo-1", results.consume(selectedPhoto))
        assertNull(results.consume(selectedPhoto))
    }

    @Test
    fun `pending names follow what is waiting`() {
        val results = NavigationResultsImpl()
        assertTrue(results.pending.value.isEmpty())

        results.post(key = selectedPhoto, value = "photo-1")
        assertEquals(setOf("selected_photo"), results.pending.value)

        results.consume(selectedPhoto)
        assertTrue(results.pending.value.isEmpty())
    }

    @Test
    fun `clear drops a value without delivering it`() {
        val results = NavigationResultsImpl()
        results.post(key = count, value = 7)

        results.clear(count)

        assertNull(results.consume(count))
        assertTrue(results.pending.value.isEmpty())
    }

    @Test
    fun `two features sharing a name fail loudly instead of reading null`() {
        val results = NavigationResultsImpl()
        val otherCount = ResultKey(name = "count", type = String::class)
        results.post(key = count, value = 7)

        assertFailsWith<IllegalStateException> { results.consume(otherCount) }
    }

    @Test
    fun `a blank key is rejected`() {
        assertFailsWith<IllegalArgumentException> { resultKey<String>(" ") }
    }
}
