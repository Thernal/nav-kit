package io.thernal.navkit.navigation.impl.domain

import io.thernal.navkit.navigation.api.presentation.argument.ArgumentKey
import io.thernal.navkit.navigation.api.presentation.argument.argumentKey
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack
import io.thernal.navkit.navigation.api.presentation.argument.whileRouteInStack
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.impl.domain.argument.NavigationArgumentsImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NavigationArgumentsImplTest {
    private data object Root : Route
    private data object CheckoutAmount : Route
    private data object CheckoutConfirm : Route

    private val draft = argumentKey<String>("checkout_draft")
    private val inCheckout = whileInStack { route -> route is CheckoutAmount || route is CheckoutConfirm }

    @Test
    fun anArgumentIsReadByEveryScreenThatAsksForIt() {
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)

        assertEquals("draft-1", arguments.get(draft))
        assertEquals("draft-1", arguments.get(draft))
    }

    @Test
    fun anArgumentSurvivesTheStackChangeThatStartsItsFlow() {
        val arguments = NavigationArgumentsImpl()
        // Put before the routes that read it are pushed: the stack the host prunes against does not
        // contain them yet, and pruning here would delete the value one frame before it is needed.
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)

        arguments.pruneFor(listOf(Root))
        arguments.pruneFor(listOf(Root, CheckoutAmount))

        assertEquals("draft-1", arguments.get(draft))
    }

    @Test
    fun anArgumentDiesWithTheFlowThatOwnedIt() {
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)

        arguments.pruneFor(listOf(Root, CheckoutAmount, CheckoutConfirm))
        arguments.pruneFor(listOf(Root, CheckoutAmount))
        assertEquals("draft-1", arguments.get(draft))

        arguments.pruneFor(listOf(Root))
        assertNull(arguments.get(draft))
    }

    @Test
    fun aScopeCanNameOneRouteType() {
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = whileRouteInStack<CheckoutConfirm>())

        arguments.pruneFor(listOf(Root, CheckoutConfirm))
        assertEquals("draft-1", arguments.get(draft))

        arguments.pruneFor(listOf(Root, CheckoutAmount))
        assertNull(arguments.get(draft))
    }

    @Test
    fun removeDropsAnArgumentBeforeItsScopeWould() {
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)

        arguments.remove(draft)

        assertNull(arguments.get(draft))
    }

    @Test
    fun twoFeaturesSharingANameFailLoudlyInsteadOfReadingNull() {
        val arguments = NavigationArgumentsImpl()
        val otherDraft = ArgumentKey(name = "checkout_draft", type = Int::class)
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)

        assertFailsWith<IllegalStateException> { arguments.get(otherDraft) }
    }

    @Test
    fun aBlankKeyIsRejected() {
        assertFailsWith<IllegalArgumentException> { argumentKey<String>(" ") }
    }

    @Test
    fun aValueReplacedInsideItsFlowStillDiesWithTheFlow() {
        // A flow that updates its own argument re-puts it while its routes are already on the
        // stack. That put used to start over as "never alive", and backing out kept it forever.
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)
        arguments.pruneFor(listOf(Root, CheckoutAmount))

        arguments.put(key = draft, value = "draft-2", scope = inCheckout)
        arguments.pruneFor(listOf(Root))

        assertNull(arguments.get(draft))
    }

    @Test
    fun aValuePutAheadOfItsRoutesStillWaitsForThem() {
        val arguments = NavigationArgumentsImpl()
        arguments.pruneFor(listOf(Root))

        arguments.put(key = draft, value = "draft-1", scope = inCheckout)
        arguments.pruneFor(listOf(Root))
        arguments.pruneFor(listOf(Root, CheckoutAmount))

        assertEquals("draft-1", arguments.get(draft))
    }

    @Test
    fun aReplacementUnderANewScopeDoesNotInheritTheOldLifetime() {
        val arguments = NavigationArgumentsImpl()
        arguments.put(key = draft, value = "draft-1", scope = inCheckout)
        arguments.pruneFor(listOf(Root, CheckoutAmount))

        arguments.put(key = draft, value = "draft-2", scope = whileRouteInStack<CheckoutConfirm>())
        arguments.pruneFor(listOf(Root))

        assertEquals("draft-2", arguments.get(draft))
    }
}
