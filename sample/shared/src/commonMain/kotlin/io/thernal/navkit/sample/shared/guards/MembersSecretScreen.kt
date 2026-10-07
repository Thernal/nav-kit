package io.thernal.navkit.sample.shared.guards

import androidx.compose.runtime.Composable
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.ListRow
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.SecondaryButton
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.GUARDS.accent

@Composable
fun MembersSecretScreen(session: SessionStore) {
    SampleScreen(
        title = "Members lounge",
        topic = Topic.GUARDS,
        howItWorks = {
            Explanation(
                "Sign out from here and watch the stack correct itself. Nothing navigates when a " +
                    "session ends: the host revalidates its own stack on the guard's invalidation, " +
                    "which is how a route that has *become* invalid leaves instead of sitting there.",
            )
        },
    ) {
        HeroCard(
            title = "Welcome back, Ada",
            emoji = "⭐",
            accent = accent,
            subtitle = "Three new things for members this week.",
        )
        ListRow(
            title = "Spring drop — early access",
            emoji = "🎟️",
            accent = accent,
            subtitle = "Opens tomorrow, 09:00",
        )
        ListRow(title = "Live Q&A with the team", emoji = "📺", accent = accent, subtitle = "Thursday, 18:00")
        ListRow(title = "Members forum", emoji = "💬", accent = accent, subtitle = "12 new threads")
        SecondaryButton(label = "Sign out", onClick = { session.signOut() })
    }
}
