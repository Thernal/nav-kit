package io.thernal.navkit.sample.shared.backoverride

import androidx.navigation3.runtime.EntryProviderScope
import io.thernal.navkit.navigation.api.presentation.host.navEntry
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider

internal class BackGraphProvider(private val drafts: ArticleDraftStore) : NavigationGraphProvider {
    override fun EntryProviderScope<Route>.provide() {
        navEntry<DraftEditorRoute> { DraftEditorScreen() }
        navEntry<ArticleHomeRoute> { ArticleHomeScreen(drafts) }
        navEntry<ArticleEditorRoute> { ArticleEditorScreen(drafts) }
    }
}
