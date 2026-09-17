package io.thernal.navkit.navigation.api.presentation.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Delivers the result posted under [key] to [onResult], once.
 *
 * A covered screen is not composed, so this runs when the user comes back to it — exactly when a
 * returning result is wanted. [onResult] belongs to the screen's state holder; the composable
 * forwards, it does not decide.
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
