package io.thernal.navkit.navigation.api.domain

import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments

/** Builds the outbound form of the same link `DeepLinkParser` reads back. */
fun buildDeepLinkUri(
    baseUrl: String,
    page: String,
    query: Map<String, String> = emptyMap(),
): String {
    return URLBuilder(baseUrl).apply {
        appendPathSegments(page)
        query.forEach { (key, value) -> parameters.append(name = key, value = value) }
    }.buildString()
}

fun DeepLinkPage.buildUri(
    baseUrl: String,
    query: Map<String, String> = emptyMap(),
): String {
    return buildDeepLinkUri(baseUrl = baseUrl, page = page, query = query)
}

inline fun <reified T> pageOf(page: String): T? where T : Enum<T>, T : DeepLinkPage {
    return enumValues<T>().firstOrNull { entry -> entry.page == page }
}
