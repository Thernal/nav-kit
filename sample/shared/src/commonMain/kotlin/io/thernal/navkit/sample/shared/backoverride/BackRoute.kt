package io.thernal.navkit.sample.shared.backoverride

import io.thernal.navkit.sample.shared.app.SampleRoute

sealed interface BackRoute : SampleRoute

data object DraftEditorRoute : BackRoute

data object ArticleHomeRoute : BackRoute

data object ArticleEditorRoute : BackRoute
