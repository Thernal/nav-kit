package io.thernal.navkit.navigation.impl.domain.argument

import io.thernal.navkit.navigation.api.presentation.argument.ArgumentKey
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentPruner
import io.thernal.navkit.navigation.api.presentation.argument.ArgumentScope
import io.thernal.navkit.navigation.api.presentation.argument.NavigationArguments
import io.thernal.navkit.navigation.api.presentation.model.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * One stored argument. [wasAlive] is the latch that makes ordering forgiving: an argument put just
 * before the routes that read it are pushed has not been alive in any stack yet, and pruning it on
 * that first stack change would delete it one frame before its flow starts.
 *
 * A put whose flow is already in the stack starts latched. A flow that updates its own argument on
 * every screen is exactly that case, and a value that started over as "never alive" would outlive
 * the flow the first time the user backed out of it.
 */
private class StoredArgument(
    val key: ArgumentKey<*>,
    val value: Any,
    val scope: ArgumentScope,
    val wasAlive: Boolean = false,
) {
    fun alive(): StoredArgument {
        if (wasAlive) {
            return this
        }
        return StoredArgument(key = key, value = value, scope = scope, wasAlive = true)
    }
}

class NavigationArgumentsImpl : NavigationArguments, ArgumentPruner {
    private val stored = MutableStateFlow<Map<String, StoredArgument>>(emptyMap())

    /** The stack the host last pruned against; `null` until the first prune. */
    private val prunedStack = MutableStateFlow<List<Route>?>(null)

    /**
     * Judged against the last pruned stack rather than copied from the value being replaced: a put
     * under the same name with a different scope has not earned the lifetime the old one had.
     */
    override fun <T : Any> put(
        key: ArgumentKey<T>,
        value: T,
        scope: ArgumentScope,
    ) {
        val isAlive = prunedStack.value?.let(scope::isAliveIn) == true
        val argument = StoredArgument(key = key, value = value, scope = scope, wasAlive = isAlive)
        stored.update { current -> current + (key.name to argument) }
    }

    override fun <T : Any> get(key: ArgumentKey<T>): T? {
        val argument = stored.value[key.name] ?: return null
        check(argument.key.type == key.type) {
            "Argument `${key.name}` was put as ${argument.key.type} and read as ${key.type}. " +
                "Two features have declared the same argument name."
        }
        @Suppress("UNCHECKED_CAST")
        return argument.value as? T
    }

    override fun remove(key: ArgumentKey<*>) {
        stored.update { current -> current - key.name }
    }

    override fun pruneFor(stack: List<Route>) {
        prunedStack.value = stack
        stored.update { current ->
            current.mapNotNull { (name, argument) ->
                when {
                    argument.scope.isAliveIn(stack) -> name to argument.alive()
                    argument.wasAlive -> null
                    else -> name to argument
                }
            }.toMap()
        }
    }
}
