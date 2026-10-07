package io.thernal.navkit.sample.shared.guards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.ContentCard
import io.thernal.navkit.sample.shared.ui.DemoButton
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.IconBadge
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SectionLabel
import io.thernal.navkit.sample.shared.ui.StatusChip
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.GUARDS.accent

/** How the community screen presents the session, signed in or not. */
private data class Account(
    val emoji: String,
    val name: String,
    val detail: String,
    val badge: String,
    val badgeColor: Color?,
    val loungeEmoji: String,
    val toggleLabel: String,
)

private val memberAccount = Account(
    emoji = "👩‍💻",
    name = "Ada Lovelace",
    detail = "ada@example.com",
    badge = "Member",
    badgeColor = SamplePalette.Green,
    loungeEmoji = "🔓",
    toggleLabel = "Sign out",
)

private val guestAccount = Account(
    emoji = "👤",
    name = "Guest",
    detail = "Not signed in",
    badge = "Guest",
    badgeColor = null,
    loungeEmoji = "🔒",
    toggleLabel = "Sign in instantly",
)

@Composable
fun MembersHomeScreen(session: SessionStore) {
    val navigator = LocalNavigator.current
    val isSignedIn by session.signedIn.collectAsState()
    val account = if (isSignedIn) {
        memberAccount
    } else {
        guestAccount
    }

    SampleScreen(
        title = "Community",
        topic = Topic.GUARDS,
        howItWorks = {
            LiveValue(label = "Signed in", value = isSignedIn.toString())
            Explanation(
                "A destination rule: the lounge requires a session, however it is reached. Open it " +
                    "signed out and the push never renders it — `AuthGuard` substitutes a sign-in " +
                    "screen before the stack reaches the host.",
            )
            Explanation(
                "Open it signed in, then sign out while you are there: the lounge leaves on its own, " +
                    "because the guard announced through `invalidations` that its answer changed.",
            )
        },
    ) {
        ContentCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconBadge(emoji = account.emoji, color = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = account.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = account.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(
                    text = account.badge,
                    color = account.badgeColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SectionLabel(text = "For members")
        ListRow(
            title = "Members lounge",
            emoji = account.loungeEmoji,
            accent = accent,
            subtitle = "Early access, live Q&As and the members forum",
            onClick = { navigator.push(MembersSecretRoute) },
        )
        DemoButton(
            label = account.toggleLabel,
            onClick = {
                if (isSignedIn) {
                    session.signOut()
                } else {
                    session.signIn()
                }
            },
        )
    }
}
