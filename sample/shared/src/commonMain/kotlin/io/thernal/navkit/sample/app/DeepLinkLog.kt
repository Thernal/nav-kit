package io.thernal.navkit.sample.app

import io.thernal.navkit.navigation.api.domain.IncomingDeepLink
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What became of the last link the application received.
 *
 * The ingress only answers whether a link was *accepted* — queued for the root to resolve. Whether a
 * handler then opened it, rejected it, or had never heard of its page is decided later and off the
 * screen that sent it. Without somewhere to put that answer it went nowhere: a rejected link read as
 * "accepted", and then nothing happened.
 */
class DeepLinkLog {
    private val mutableLast = MutableStateFlow<String?>(null)

    val last: StateFlow<String?> = mutableLast.asStateFlow()

    fun record(
        link: IncomingDeepLink,
        outcome: DeepLinkOutcome,
    ) {
        val result = when (outcome) {
            is DeepLinkOutcome.Navigate -> "opened a stack of ${outcome.routes.size}"
            is DeepLinkOutcome.Rejected -> "rejected — ${outcome.reason}"
            DeepLinkOutcome.NotFound -> "not found — it starts with no registered base, or no handler owns its page"
        }
        mutableLast.value = "${link.uri} (${link.source.name.lowercase()}): $result"
    }

    fun recordRefused(uri: String) {
        mutableLast.value = "\"$uri\": refused by the ingress before any handler saw it"
    }
}
