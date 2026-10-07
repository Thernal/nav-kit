package io.thernal.navkit.navigation.impl.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.domain.back.BackDispatcherImpl
import io.thernal.navkit.navigation.impl.domain.guard.NavigationGuardRunnerImpl
import io.thernal.navkit.navigation.impl.domain.navigator.BackStackNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostStackWriterTest {
    private data object Root : Route
    private data object Details : Route
    private data object Edit : Route
    private data object Elsewhere : Route

    /** An owner that answers late, the way a `StateFlow` collected into composition does. */
    private class LaggingOwner(initial: ImmutableList<Route>) {
        var stack: ImmutableList<Route> = initial
            private set

        val writer = HostStackWriter<Route> { written -> stack = written }

        /** What the host renders until the next composition hands [stack] over. */
        var rendered: ImmutableList<Route> = initial
            private set

        val navigator = BackStackNavigator(
            buildBackStack = { builder ->
                val mutable = writer.newestOr(rendered).toMutableList()
                mutable.builder()
                writer.write(mutable.toImmutableList())
            },
            resolveCanPop = { writer.newestOr(rendered).size > 1 },
            resolveGuardRunner = { NavigationGuardRunnerImpl(emptyList()) },
            backDispatcher = BackDispatcherImpl(),
        )

        init {
            writer.acknowledge(initial)
        }

        fun compose(): Boolean {
            rendered = stack
            return writer.acknowledge(stack)
        }
    }

    @Test
    fun `two commands in one frame build on each other`() {
        // The navigator used to build on the rendered stack, so the second pop undid nothing new
        // and `popBack(2)` popped once.
        val owner = LaggingOwner(persistentListOf(Root, Details, Edit))

        assertTrue(owner.navigator.popBack(2))

        assertEquals(listOf<Route>(Root), owner.stack)
    }

    @Test
    fun `a pop followed by a push in one frame keeps the pop`() {
        val owner = LaggingOwner(persistentListOf(Root, Details))

        owner.navigator.popBack()
        owner.navigator.push(Edit)

        assertEquals(listOf<Route>(Root, Edit), owner.stack)
    }

    @Test
    fun `an own write handed back is not an external change`() {
        val owner = LaggingOwner(persistentListOf(Root))
        owner.navigator.push(Details)

        assertFalse(owner.compose())
        assertEquals(listOf<Route>(Root, Details), owner.writer.newestOr(owner.rendered))
    }

    @Test
    fun `a skipped intermediate write is still own`() {
        // A conflated owner hands back only the last of several writes.
        val owner = LaggingOwner(persistentListOf(Root))
        owner.navigator.push(Details)
        owner.navigator.push(Edit)

        assertFalse(owner.compose())
    }

    @Test
    fun `each write is confirmed in turn by an owner that skips nothing`() {
        val writer = HostStackWriter<Route> { }
        val first = persistentListOf<Route>(Root, Details)
        val second = persistentListOf<Route>(Root)
        writer.acknowledge(second)
        writer.write(first)
        writer.write(second)

        assertFalse(writer.acknowledge(first))
        assertFalse(writer.acknowledge(second))
    }

    @Test
    fun `a stack from elsewhere is external and drops the writes still waiting`() {
        val owner = LaggingOwner(persistentListOf(Root))
        owner.navigator.push(Details)
        val foreign = persistentListOf<Route>(Root, Elsewhere)

        assertTrue(owner.writer.acknowledge(foreign))
        assertEquals(listOf<Route>(Root, Elsewhere), owner.writer.newestOr(foreign))
    }

    @Test
    fun `the first stack and an unchanged one are nobodys change`() {
        val writer = HostStackWriter<Route> { }
        val stack = persistentListOf<Route>(Root)

        assertFalse(writer.acknowledge(stack))
        assertFalse(writer.acknowledge(listOf<Route>(Root).toImmutableList()))
    }
}
