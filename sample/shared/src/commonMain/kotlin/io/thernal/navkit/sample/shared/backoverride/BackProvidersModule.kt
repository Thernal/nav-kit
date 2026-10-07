package io.thernal.navkit.sample.shared.backoverride

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.thernal.navkit.navigation.api.presentation.guard.NavigationGuard
import io.thernal.navkit.navigation.api.presentation.navigator.NavigationGraphProvider
import io.thernal.navkit.sample.shared.app.ExampleKind
import io.thernal.navkit.sample.shared.app.SampleExample
import io.thernal.navkit.sample.shared.ui.Topic

@BindingContainer
@ContributesTo(AppScope::class)
interface BackProvidersModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideArticleDraftStore(): ArticleDraftStore {
            return ArticleDraftStore()
        }

        /**
         * Contributed into the kit's own guard multibinding. The application graph is the only
         * place that knows both the guard and the state it reads; neither the navigation module nor
         * the screens have to.
         */
        @Provides
        @IntoSet
        fun provideUnsavedWorkGuard(drafts: ArticleDraftStore): NavigationGuard {
            return UnsavedWorkGuard(drafts)
        }

        @Provides
        @IntoSet
        fun provideBackGraphProvider(drafts: ArticleDraftStore): NavigationGraphProvider {
            return BackGraphProvider(drafts)
        }

        @Provides
        @IntoSet
        fun provideDraftExample(): SampleExample {
            return SampleExample(
                topic = Topic.BACK_HANDLING,
                kind = ExampleKind.SIMPLE,
                title = "Confirm before leaving",
                summary = "A screen intercepts back, for as long as it is composed.",
                route = DraftEditorRoute,
            )
        }

        @Provides
        @IntoSet
        fun provideArticleExample(): SampleExample {
            return SampleExample(
                topic = Topic.BACK_HANDLING,
                kind = ExampleKind.ADVANCED,
                title = "Unsaved work",
                summary = "A transition guard refuses every way out, gesture or not.",
                route = ArticleHomeRoute,
            )
        }
    }
}
