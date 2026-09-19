package io.thernal.navkit.sample.guards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ContentCard
import io.thernal.navkit.sample.ui.DemoButton
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.Green
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.IconBadge
import io.thernal.navkit.sample.ui.Indigo
import io.thernal.navkit.sample.ui.ListRow
import io.thernal.navkit.sample.ui.LiveValue
import io.thernal.navkit.sample.ui.PrimaryButton
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SecondaryButton
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.StatusChip
import io.thernal.navkit.sample.ui.Topic

private const val PIN_LENGTH = 4

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
    badgeColor = Green,
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

private val keypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", "⌫"),
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

@Composable
fun SignInScreen(
    route: SignInRoute,
    session: SessionStore,
    isEmbedded: Boolean = false,
) {
    val navigator = LocalNavigator.current
    var email by rememberSaveable { mutableStateOf("ada@example.com") }
    var password by rememberSaveable { mutableStateOf("analytical-engine") }

    SampleScreen(
        title = "Sign in",
        topic = Topic.GUARDS,
        // Inside a tab, the tabs screen already has the top bar.
        showTopBar = !isEmbedded,
        howItWorks = {
            LiveValue(
                label = "Was heading to",
                value = route.next?.let { next -> next::class.simpleName } ?: "nowhere",
            )
            Explanation(
                "Nothing pushed this screen — the guard put it here. It redirected by " +
                    "*substitution*, replacing the protected route where it sat, and carried the " +
                    "original destination on `SignInRoute(next = …)`, so signing in continues " +
                    "there with `navigator.replace(next)`.",
            )
        },
    ) {
        HeroCard(
            title = "Sign in to continue",
            emoji = "🔑",
            accent = Indigo,
            subtitle = "You'll go straight on to ${destinationName(route.next)}.",
        )
        OutlinedTextField(
            value = email,
            onValueChange = { entered -> email = entered },
            label = { Text(text = "Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { entered -> password = entered },
            label = { Text(text = "Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            label = "Sign in",
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = {
                session.signIn()
                val next = route.next
                if (next == null) {
                    navigator.popBack()
                } else {
                    navigator.replace(next)
                }
            },
        )
    }
}

@Composable
fun VaultLobbyScreen(session: PinSession) {
    val navigator = LocalNavigator.current
    val isLocked by session.locked.collectAsState()
    val vaultState = if (isLocked) {
        "🔒" to "Session expired — PIN required"
    } else {
        "🔐" to "PIN protected"
    }

    SampleScreen(
        title = "Accounts",
        topic = Topic.GUARDS,
        howItWorks = {
            LiveValue(label = "Session locked", value = isLocked.toString())
            Explanation(
                "A 401 arrives mid-session: the destination is still right, the session is not. " +
                    "Simulate one, then open the vault — the guard *defers* instead of redirecting, " +
                    "shows the PIN pad meanwhile, and continues to the vault once the PIN is right. " +
                    "Get it wrong and the vault is refused instead.",
            )
        },
    ) {
        HeroCard(
            title = "€14,302.55",
            emoji = "🏦",
            accent = Indigo,
            subtitle = "Total balance across 2 accounts",
        )
        SectionLabel(text = "Accounts")
        ListRow(title = "Everyday", emoji = "💳", accent = Indigo, subtitle = "•••• 4242", trailing = "€1,822.55")
        ListRow(
            title = "Savings vault",
            emoji = vaultState.first,
            accent = accent,
            subtitle = vaultState.second,
            trailing = "€12,480.00",
            onClick = { navigator.push(VaultRoute) },
        )
        DemoButton(label = "Simulate a 401 — session expired", onClick = { session.lock() })
    }
}

@Composable
fun VaultScreen(session: PinSession) {
    SampleScreen(
        title = "Savings vault",
        topic = Topic.GUARDS,
        howItWorks = {
            Explanation(
                "Simulate a 401 from here: the guard's `meanwhile` replaces this screen with the PIN " +
                    "pad in one step, so the balance is off the screen while the PIN is asked for — " +
                    "and the same stack comes back afterwards, not a rebuilt one.",
            )
        },
    ) {
        HeroCard(
            title = "€12,480.00",
            emoji = "🔐",
            accent = accent,
            subtitle = "Savings vault · 2.1% interest",
        )
        SectionLabel(text = "Recent activity")
        ListRow(title = "Interest", emoji = "📈", accent = Green, subtitle = "1 Sep", trailing = "+€18.20")
        ListRow(title = "From Everyday", emoji = "↔️", accent = Indigo, subtitle = "28 Aug", trailing = "+€500.00")
        ListRow(title = "Round-ups", emoji = "🪙", accent = accent, subtitle = "This month", trailing = "+€3.40")
        DemoButton(label = "Simulate a 401 — session expired", onClick = { session.lock() })
    }
}

@Composable
fun PinEntryScreen(session: PinSession) {
    var pin by rememberSaveable { mutableStateOf("") }

    SampleScreen(
        title = "Security check",
        topic = Topic.GUARDS,
        howItWorks = {
            Explanation("The PIN is `1234`. Anything else and the guard refuses the vault.")
            Explanation(
                "The guard is suspended, waiting for this screen to answer. This route is a " +
                    "`TransientRoute`: if the process died right now, the stack would be restored " +
                    "without it — otherwise it would come back as a prompt with no coroutine left " +
                    "to answer it, and no way out.",
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(emoji = "🔒", color = accent, size = 64.dp)
            Text(text = "Enter your PIN", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Your session expired. Enter your 4-digit PIN to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            PinDots(entered = pin.length)
            Keypad(
                onKey = { key ->
                    pin = when {
                        key == "⌫" -> pin.dropLast(1)
                        pin.length < PIN_LENGTH -> pin + key
                        else -> pin
                    }
                    if (pin.length == PIN_LENGTH) {
                        session.submit(pin)
                        pin = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun PinDots(entered: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(PIN_LENGTH) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < entered) {
                            accent
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
            )
        }
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        keypadRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    KeypadKey(key = key, onKey = onKey)
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    key: String,
    onKey: (String) -> Unit,
) {
    val keyModifier = Modifier.size(72.dp).clip(CircleShape)
    if (key.isEmpty()) {
        Box(modifier = keyModifier)
        return
    }
    Box(
        modifier = keyModifier
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onKey(key) },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = key, style = MaterialTheme.typography.headlineSmall)
    }
}

/** A reader-facing name for where the sign-in continues to. */
private fun destinationName(next: Route?): String {
    if (next == null) {
        return "where you were"
    }
    if (next == MembersSecretRoute) {
        return "the members lounge"
    }
    val name = next::class.simpleName.orEmpty().removeSuffix("Route").removeSuffix("Tab")
    return name.ifEmpty { "where you were going" }
}
