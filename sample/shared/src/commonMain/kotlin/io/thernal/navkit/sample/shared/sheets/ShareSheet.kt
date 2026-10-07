package io.thernal.navkit.sample.shared.sheets

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.SheetStep
import io.thernal.navkit.sample.shared.ui.Topic

private data class ShareTarget(
    val name: String,
    val emoji: String,
    val detail: String,
)

private val shareTargets = listOf(
    ShareTarget(name = "Copy link", emoji = "🔗", detail = "navkit.example/p/8213"),
    ShareTarget(name = "Messages", emoji = "💬", detail = "Send to a conversation"),
    ShareTarget(name = "Mail", emoji = "✉️", detail = "Compose a new message"),
)

@Composable
fun ShareSheet() {
    val navigator = LocalNavigator.current
    val results = LocalNavigationResults.current

    SheetStep(title = "Share", subtitle = "Sourdough, day 14") {
        shareTargets.forEach { target ->
            ListRow(
                title = target.name,
                emoji = target.emoji,
                accent = Topic.BOTTOM_SHEETS.accent,
                subtitle = target.detail,
                onClick = {
                    results.post(key = SharedWith, value = target.name)
                    navigator.popBack()
                },
            )
        }
        SecondaryButton(label = "Cancel", onClick = { navigator.popBack() })
    }
}
