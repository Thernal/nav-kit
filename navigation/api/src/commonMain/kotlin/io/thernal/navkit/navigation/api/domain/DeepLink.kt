package io.thernal.navkit.navigation.api.domain

/**
 * A parsed inbound link. [pathSegments] is what follows the registered [base], so [page] is the same
 * for every registered form of one link; [host] is the host as written.
 */
data class DeepLink(
    val raw: String,
    val base: DeepLinkBase,
    val scheme: String,
    val host: String?,
    val pathSegments: List<String>,
    val query: Map<String, List<String>>,
) {
    val page: String? get() = pathSegments.firstOrNull()

    fun query(key: String): String? {
        return query[key]?.firstOrNull()
    }
}
