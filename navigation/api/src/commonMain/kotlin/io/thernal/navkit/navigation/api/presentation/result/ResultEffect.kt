package io.thernal.navkit.navigation.api.presentation.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Delivers the result posted under [key] to [onResult], once.
 *
 * Navigation3 composes only the entries of the current scene, so a screen that is covered is not
 * composed and this effect does not run — it runs when the user comes back to it, which is exactly
 * when a returning result is wanted. A result posted while this screen is already on top is
 * delivered immediately.
 *
 * [onResult] belongs to the screen's state holder. The composable forwards; it does not decide.
 */
@Composable
fun <T : Any> ResultEffect(
    key: ResultKey<T>,
    onResult: (T) -> Unit,
) {
    val results = LocalNavigationResults.current
    val currentOnResult by rememberUpdatedState(onResult)
    val pending by results.pending.collectAsState()

    LaunchedEffect(key1 = results, key2 = key, key3 = pending) {
        if (key.name !in pending) {
            return@LaunchedEffect
        }
        val value = results.consume(key) ?: return@LaunchedEffect
        currentOnResult(value)
    }
}
