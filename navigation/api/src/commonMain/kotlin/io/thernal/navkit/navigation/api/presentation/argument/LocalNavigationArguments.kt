package io.thernal.navkit.navigation.api.presentation.argument

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The application's [NavigationArguments]. `wiring` contributes it to the graph's `ProvidedValue<*>`
 * set and the composition root installs it, the same way `LocalNavigationHostRenderer` is
 * installed.
 */
val LocalNavigationArguments = staticCompositionLocalOf<NavigationArguments> { NoOpNavigationArguments }

/** Holds nothing, so a screen composed outside the app graph — a preview — still composes. */
private object NoOpNavigationArguments : NavigationArguments {
    override fun <T : Any> put(
        key: ArgumentKey<T>,
        value: T,
        scope: ArgumentScope,
    ) {
        // Intentionally empty: a preview has no flow to carry a value across.
    }

    override fun <T : Any> get(key: ArgumentKey<T>): T? {
        return null
    }

    override fun remove(key: ArgumentKey<*>) {
        // Intentionally empty: nothing was ever put.
    }
}
