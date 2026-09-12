package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.result.consume
import io.thernal.navkit.navigation.impl.domain.result.NavigationResultStoreImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NavigationResultStoreImplTest {
    @Test
    fun aResultIsReadOnceAndThenGone() {
        val store = NavigationResultStoreImpl()
        store.set("selected_photo", "photo-1")

        assertEquals("photo-1", store.consume<String>("selected_photo"))
        assertNull(store.consume<String>("selected_photo"))
    }

    @Test
    fun aMismatchedTypeReadsNullAndLeavesTheValueInPlace() {
        val store = NavigationResultStoreImpl()
        store.set("count", 7)

        assertNull(store.consume<String>("count"))
        assertEquals(7, store.consume<Int>("count"))
    }

    @Test
    fun aBlankKeyIsRejected() {
        val store = NavigationResultStoreImpl()

        assertFailsWith<IllegalArgumentException> { store.set(" ", "value") }
    }
}
