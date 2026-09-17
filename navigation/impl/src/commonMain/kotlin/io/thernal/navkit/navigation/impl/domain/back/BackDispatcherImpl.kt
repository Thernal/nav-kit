package io.thernal.navkit.navigation.impl.domain.back

import io.thernal.navkit.navigation.api.presentation.back.BackCallback
import io.thernal.navkit.navigation.api.presentation.back.BackDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Callbacks live in a [MutableStateFlow] over an immutable list: `update` is a compare-and-set loop
 * on every platform, and [dispatch] walks a snapshot, so a callback that unregisters itself
 * mid-dispatch cannot corrupt the walk.
 */
class BackDispatcherImpl : BackDispatcher {
    private val callbacks = MutableStateFlow<List<BackCallback>>(emptyList())

    // Back is dispatched from the UI thread, so a plain flag is enough.
    private var isDispatching = false

    override fun register(callback: BackCallback): AutoCloseable {
        callbacks.update { current -> current + callback }
        return AutoCloseable { callbacks.update { current -> current - callback } }
    }

    /**
     * A nested dispatch consumes nothing, so a callback that lets back through by calling `popBack()`
     * from its own handler falls through to the stack instead of being dispatched to forever.
     */
    override fun dispatch(): Boolean {
        if (isDispatching) {
            return false
        }
        isDispatching = true
        try {
            return callbacks.value.asReversed().any(BackCallback::handle)
        } finally {
            isDispatching = false
        }
    }

    override fun hasCallbacks(): Boolean {
        return callbacks.value.isNotEmpty()
    }
}
