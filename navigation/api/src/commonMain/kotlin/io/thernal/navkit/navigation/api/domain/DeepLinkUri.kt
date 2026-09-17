package io.thernal.navkit.navigation.api.domain

import io.ktor.http.encodeURLParameter

/**
 * Builds the link that `DeepLinkParser` reads back, against the same registered [base], as [page]
 * with [query]: `navkit://profile?id=42`, `https://example.com/profile?id=42`.
 *
 * On a base with a host the page follows the base's path; on an app-scheme base it takes the host's
 * position, which is where the parser looks for it.
 */
fun buildDeepLinkUri(
    base: DeepLinkBase,
    page: String,
    query: Map<String, String> = emptyMap(),
): String {
    require(page.isNotBlank()) { "A deep link page cannot be blank" }
    // An app-scheme base ends at `://`, where the page takes the host's position.
    val separator = if (base.host == null) {
        ""
    } else {
        "/"
    }
    val link = base.uri + separator + page.encodeURLParameter()
    if (query.isEmpty()) {
        return link
    }
    val parameters = query.entries.joinToString(separator = "&") { (name, value) ->
        "${name.encodeURLParameter()}=${value.encodeURLParameter()}"
    }
    return "$link?$parameters"
}

fun DeepLinkPage.buildUri(
    base: DeepLinkBase,
    query: Map<String, String> = emptyMap(),
): String {
    return buildDeepLinkUri(base = base, page = page, query = query)
}

inline fun <reified T> pageOf(page: String): T? where T : Enum<T>, T : DeepLinkPage {
    return enumValues<T>().firstOrNull { entry -> entry.page == page }
}
