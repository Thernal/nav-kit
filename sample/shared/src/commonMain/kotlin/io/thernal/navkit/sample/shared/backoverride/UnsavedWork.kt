package io.thernal.navkit.sample.shared.backoverride

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason

/** The application's own vocabulary for a refusal; the navigation layer only ever renders it. */
data object UnsavedWork : BlockReason {
    override val message: String = "The article has unsaved changes"
}
