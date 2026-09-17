package io.thernal.navkit.navigation.api.presentation.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Delivers the result posted under [key] to [onResult], once — when the user comes back to this
 * screen, since a covered one is not composed. [onResult] belongs to the state holder.
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
