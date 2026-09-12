package io.thernal.navkit.navigation.impl.domain.back

import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Callbacks are held in a [MutableStateFlow] over an immutable list rather than a synchronized
 * collection: `update` is a compare-and-set loop on every platform, and [dispatch] iterates a
 * snapshot, so a callback that unregisters itself while being dispatched cannot corrupt the walk.
 */
class BackDispatcherImpl : BackDispatcher {
    private val callbacks = MutableStateFlow<List<BackCallback>>(emptyList())

    override fun register(callback: BackCallback): AutoCloseable {
        callbacks.update { current -> current + callback }
        return AutoCloseable { callbacks.update { current -> current - callback } }
    }

    override fun dispatch(): Boolean {
        return callbacks.value.asReversed().any(BackCallback::handle)
    }

    override fun hasCallbacks(): Boolean {
        return callbacks.value.isNotEmpty()
    }
}
