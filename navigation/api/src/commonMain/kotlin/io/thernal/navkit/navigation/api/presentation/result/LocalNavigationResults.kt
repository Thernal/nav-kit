package io.thernal.navkit.navigation.api.presentation.result

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The application's [NavigationResults], contributed by `wiring` and installed at the root. */
val LocalNavigationResults = staticCompositionLocalOf<NavigationResults> { NoOpNavigationResults }

/** Holds nothing, so a screen composed outside the app graph — a preview — still composes. */
private object NoOpNavigationResults : NavigationResults {
    override val pending: StateFlow<Set<String>> = MutableStateFlow(emptySet())

    override fun <T : Any> post(
        key: ResultKey<T>,
        value: T,
    ) {
        // Intentionally empty: a preview has nobody to deliver to.
    }

    override fun <T : Any> consume(key: ResultKey<T>): T? {
        return null
    }

    override fun clear(key: ResultKey<*>) {
        // Intentionally empty: nothing was ever posted.
    }
}
