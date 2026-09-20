package io.thernal.navkit.sample.sheets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.navigation.api.presentation.result.LocalNavigationResults
import io.thernal.navkit.navigation.api.presentation.result.ResultEffect
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.KeyValueRow
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.PrimaryButton
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.SheetStep
import io.thernal.navkit.sample.ui.Topic

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

/**
 * The whole of a bottom sheet: an ordinary route pushed with the ordinary `push`, registered with
 * `bottomSheetEntry`. What the entry changes is the surface, not the stack.
 */
@Composable
fun PostScreen() {
    val navigator = LocalNavigator.current
    var sharedWith by rememberSaveable { mutableStateOf<String?>(null) }

    // An overlay leaves this screen composed, so the row updates under the open sheet.
    ResultEffect(SharedWith) { target -> sharedWith = target }

    SampleScreen(
        title = "Field notes",
        topic = Topic.BOTTOM_SHEETS,
        howItWorks = {
            Explanation(
                "`bottomSheetEntry<ShareSheetRoute> { ShareSheet() }` is the only line that differs " +
                    "from a full screen. It adds one metadata key, and the host's bottom-sheet scene " +
                    "claims the entry that carries it — the route, the `push` and the back stack are " +
                    "unchanged.",
            )
            Explanation(
                "A sheet is an *overlay*, so the screen under it keeps composing. That is what makes " +
                    "the shared-with row change before the sheet closes: `ResultEffect` re-runs on the " +
                    "post, not on the return. A pushed full screen would deliver the same value a " +
                    "moment later, when it is uncovered.",
            )
            Explanation(
                "The scrim, the rounded panel and the drag handle are in `SheetSurface`, installed " +
                    "once on the root host as its `bottomSheetContainer`. The kit draws nothing: what " +
                    "a sheet looks like belongs to a design system, not to a navigation library.",
            )
        },
    ) {
        HeroCard(
            title = "Sourdough, day 14",
            emoji = "🍞",
            accent = Topic.BOTTOM_SHEETS.accent,
            subtitle = "Field notes · 3 min read",
        )
        ContentCard {
            Text(
                text = "The starter doubled in four hours today, which is the first time it has " +
                    "managed that since the kitchen got cold.",
                style = MaterialTheme.typography.bodyMedium,
            )
            KeyValueRow(key = "Shared with", value = sharedWith ?: "Nobody yet")
        }
        SectionLabel(text = "Actions")
        PrimaryButton(
            label = "Share",
            onClick = { navigator.push(ShareSheetRoute) },
            color = Topic.BOTTOM_SHEETS.accent,
        )
    }
}

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
