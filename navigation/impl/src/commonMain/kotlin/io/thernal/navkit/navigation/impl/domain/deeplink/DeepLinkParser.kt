package io.thernal.navkit.navigation.impl.domain.deeplink

import io.ktor.http.Url
import io.ktor.http.decodeURLPart
import io.thernal.navkit.navigation.api.domain.DeepLink

/** Schemes whose host is a domain name rather than the first page of the link. */
private val WEB_SCHEMES = setOf("http", "https")

private val SCHEME_PATTERN = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*):")

/**
 * Reads a raw link into a [DeepLink].
 *
 * The one rule worth stating: on a custom scheme the host **is** the first page — `navkit://booking/42`
 * yields `["booking", "42"]` — because there is no domain there to be a host. On `http(s)` the host is
 * a domain and only the path counts, so `https://example.com/booking/42` yields the same
 * `["booking", "42"]`. Both forms of the same link therefore resolve to the same handler, which is
 * the point: a feature declares its page once and gets the app-scheme and the web form for free.
 *
 * Derived from the scheme rather than from a configured list of app schemes: a list is one more
 * thing to keep in sync with the manifest and the `Info.plist`, and it buys nothing — no scheme
 * outside http(s) has a meaningful host.
 */
fun parseDeepLink(raw: String): DeepLink? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return null
    }
    val url = runCatching { Url(trimmed) }.getOrNull() ?: return null
    val scheme = SCHEME_PATTERN.find(trimmed)?.groupValues?.get(1)?.lowercase()
    val host = url.host.takeIf(String::isNotBlank)

    val pathSegments = buildList {
        if (host != null && scheme !in WEB_SCHEMES) {
            add(host)
        }
        url.encodedPath
            .split('/')
            .filter(String::isNotBlank)
            .forEach { segment -> add(segment.decodeURLPart()) }
    }
    if (pathSegments.isEmpty()) {
        return null
    }

    val query = url.parameters.entries().associate { entry -> entry.key to entry.value }
    return DeepLink(
        raw = trimmed,
        scheme = scheme,
        host = host,
        pathSegments = pathSegments,
        query = query,
    )
}
