package io.thernal.navkit.navigation.api.domain

import io.ktor.http.decodeURLPart
import io.ktor.http.encodeURLParameter

/** `scheme://host/path` — the host and the path optional, a query or a fragment not allowed. */
private val BASE_PATTERN = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*)://([^/?#]*)(/[^?#]*)?$")

private const val SCHEME_GROUP = 1
private const val HOST_GROUP = 2
private const val PATH_GROUP = 3

/** Schemes whose host is a domain, which a base therefore has to name. */
private val WEB_SCHEMES = setOf("http", "https")

/**
 * A prefix the application's links start with — `navkit://`, `https://example.com/app`. A link is
 * read by removing the most specific registered base it starts with; one matching none is not the
 * application's. Scheme and host compare case-insensitively, the path segment by segment; a web base
 * must name a host, and a base without one has no path.
 */
class DeepLinkBase(uri: String) {
    /** Lower-cased. */
    val scheme: String

    /** Lower-cased, with its port if the base names one; `null` for an app-scheme base such as `navkit://`. */
    val host: String?

    /** Decoded. */
    val pathSegments: List<String>

    /** The normalized form, which [buildDeepLinkUri] appends a page to. */
    val uri: String

    init {
        val match = requireNotNull(BASE_PATTERN.matchEntire(uri.trim())) {
            "A deep link base is scheme://host/path with no query or fragment, got '$uri'"
        }
        scheme = match.groups[SCHEME_GROUP]?.value.orEmpty().lowercase()
        host = match.groups[HOST_GROUP]?.value?.lowercase()?.takeIf(String::isNotEmpty)
        pathSegments = match.groups[PATH_GROUP]?.value.orEmpty()
            .split('/')
            .filter(String::isNotBlank)
            .map { segment -> segment.decodeURLPart() }

        require(host != null || scheme !in WEB_SCHEMES) {
            "A web deep link base needs a host, got '$uri'"
        }
        require(host != null || pathSegments.isEmpty()) {
            "A deep link base without a host cannot have a path — the page goes where the host would be, got '$uri'"
        }

        val encodedPath = pathSegments.joinToString(separator = "") { segment -> "/${segment.encodeURLParameter()}" }
        this.uri = "$scheme://${host.orEmpty()}$encodedPath"
    }

    override fun equals(other: Any?): Boolean {
        return other is DeepLinkBase && other.uri == uri
    }

    override fun hashCode(): Int {
        return uri.hashCode()
    }

    override fun toString(): String {
        return uri
    }
}
