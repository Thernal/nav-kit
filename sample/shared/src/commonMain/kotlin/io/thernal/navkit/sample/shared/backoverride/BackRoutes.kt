package io.thernal.navkit.sample.backoverride

import io.thernal.navkit.navigation.api.presentation.guard.BlockReason
import io.thernal.navkit.sample.app.SampleRoute

sealed interface BackRoute : SampleRoute

data object DraftEditorRoute : BackRoute

data object ArticleHomeRoute : BackRoute

data object ArticleEditorRoute : BackRoute

/** The application's own vocabulary for a refusal; the navigation layer only ever renders it. */
data object UnsavedWork : BlockReason {
    override val message: String = "The article has unsaved changes"
}
