package io.thernal.navkit.sample.shared.basics

import io.thernal.navkit.navigation.api.presentation.navigator.NavigationOutcome

internal fun NavigationOutcome.describe(): String {
    return when (this) {
        is NavigationOutcome.Applied -> "applied · ${stack.size} route(s)"
        is NavigationOutcome.Rewritten -> "rewritten by a guard · ${reason?.message ?: "no reason given"}"
        is NavigationOutcome.Deferred -> "deferred · a guard needs time"
    }
}
