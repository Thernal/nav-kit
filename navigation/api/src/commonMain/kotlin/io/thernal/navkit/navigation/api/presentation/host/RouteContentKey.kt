package io.thernal.navkit.navigation.api.presentation.host

import io.thernal.navkit.navigation.api.presentation.model.Route

/**
 * What Navigation3 keys an entry's content by. Its default is `toString()`, which two `data object Main`
 * routes of different features share — the transition between them would not run. The full name tells
 * them apart, and a `data class` route's `toString()` adds its arguments.
 */
@PublishedApi
internal fun routeContentKey(route: Route): String {
    return "${route::class.qualifiedName}:$route"
}
