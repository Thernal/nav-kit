package io.thernal.navkit.navigation.impl.domain.deeplink

import io.ktor.http.decodeURLPart
import io.ktor.http.parseQueryString
import io.thernal.navkit.navigation.api.domain.DeepLink
import io.thernal.navkit.navigation.api.domain.DeepLinkBase

/** `scheme://host/path?query#fragment`, everything after the scheme optional. */
private val LINK_PATTERN = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*)://([^/?#]*)([^?#]*)(?:\\?([^#]*))?(?:#.*)?$")

private const val SCHEME_GROUP = 1
private const val HOST_GROUP = 2
private const val PATH_GROUP = 3
private const val QUERY_GROUP = 4

/** A link split at its host, before any base is removed. */
private class LinkParts(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
)

/**
 * Reads a raw link into a [DeepLink], against the bases the application registered.
 *
 * **The most specific registered base the link starts with is removed, and what follows is the
 * page.** `navkit://booking/42` against `navkit://` and `https://example.com/booking/42` against
 * `https://example.com` both yield `["booking", "42"]`, so a feature declares its page once and every
 * registered form of the link reaches it. On an app scheme that leaves the host in the page's
 * position — there is no domain there to be a host.
 *
 * `null` when the link is malformed, starts with no registered base, or names no page after it. The
 * registered list has to agree with `AndroidManifest.xml` and `Info.plist`; why it is registered
 * rather than derived from the scheme is in `navigation/README.md`, "Deep links".
 */
fun parseDeepLink(
    raw: String,
    bases: Collection<DeepLinkBase>,
): DeepLink? {
    val trimmed = raw.trim()
    val match = LINK_PATTERN.matchEntire(trimmed) ?: return null
    val parts = runCatching {
        LinkParts(
            scheme = match.groups[SCHEME_GROUP]?.value.orEmpty().lowercase(),
            host = match.groups[HOST_GROUP]?.value.orEmpty().decodeURLPart(),
            pathSegments = match.groups[PATH_GROUP]?.value.orEmpty()
                .split('/')
                .filter(String::isNotBlank)
                .map { segment -> segment.decodeURLPart() },
        )
    }.getOrNull() ?: return null

    val (base, pathSegments) = bases
        .mapNotNull { candidate -> candidate.remainderOf(parts)?.let { remainder -> candidate to remainder } }
        .maxByOrNull { (candidate, _) -> candidate.specificity() }
        ?: return null
    if (pathSegments.isEmpty()) {
        return null
    }

    val query = parseQueryString(match.groups[QUERY_GROUP]?.value.orEmpty())
        .entries()
        .associate { entry -> entry.key to entry.value }
    return DeepLink(
        raw = trimmed,
        base = base,
        scheme = parts.scheme,
        host = parts.host.takeIf(String::isNotEmpty),
        pathSegments = pathSegments,
        query = query,
    )
}

/** What follows this base in [link], or `null` when the link does not start with it. */
private fun DeepLinkBase.remainderOf(link: LinkParts): List<String>? {
    if (link.scheme != scheme) {
        return null
    }
    val baseHost = host
    if (baseHost == null) {
        // An app-scheme base names no host, so the link's host is the first thing after the base.
        return listOfNotNull(link.host.takeIf(String::isNotEmpty)) + link.pathSegments
    }
    if (!link.host.equals(other = baseHost, ignoreCase = true)) {
        return null
    }
    if (link.pathSegments.take(pathSegments.size) != pathSegments) {
        return null
    }
    return link.pathSegments.drop(pathSegments.size)
}

/** How much of a link this base accounts for, so the longest matching base wins. */
private fun DeepLinkBase.specificity(): Int {
    val hostCount = if (host == null) {
        0
    } else {
        1
    }
    return hostCount + pathSegments.size
}
