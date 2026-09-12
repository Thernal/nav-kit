package io.thernal.navkit.navigation.api.domain

/** A parsed inbound link, independent of how it reached the app. */
data class DeepLink(
    val raw: String,
    val scheme: String?,
    val host: String?,
    val pathSegments: List<String>,
    val query: Map<String, List<String>>,
) {
    val page: String? get() = pathSegments.firstOrNull()

    fun query(key: String): String? {
        return query[key]?.firstOrNull()
    }
}
