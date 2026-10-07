package io.thernal.navkit.sample.shared.backoverride

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.BACK_HANDLING.accent

@Composable
fun ArticleHomeScreen(drafts: ArticleDraftStore) {
    val navigator = LocalNavigator.current

    SampleScreen(
        title = "My articles",
        topic = Topic.BACK_HANDLING,
        howItWorks = {
            Explanation(
                "The editor cannot be left with unsaved work — enforced by a guard, not by a " +
                    "screen. A `NavigationBackHandler` only fires while its screen is composed, so " +
                    "it cannot stop a jump that skips the screen — a deep link, a `replaceAll`, a " +
                    "guard rewrite. A transition guard can, because it sees every stack change.",
            )
        },
    ) {
        SectionLabel(text = "Drafts")
        ListRow(
            title = "Why back stacks are state",
            emoji = "📝",
            accent = accent,
            subtitle = drafts.saved.ifBlank { "Nothing saved yet — tap to start writing" },
            onClick = { navigator.push(ArticleEditorRoute) },
        )
    }
}
