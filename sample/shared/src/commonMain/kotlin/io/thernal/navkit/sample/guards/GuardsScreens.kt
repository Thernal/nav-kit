package io.thernal.navkit.sample.guards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

@Composable
fun MembersHomeScreen(session: SessionStore) {
    val navigator = LocalNavigator.current
    val isSignedIn by session.signedIn.collectAsState()

    ExampleScaffold(
        title = "Members area",
        subtitle = "A destination rule: the secret page requires a session, however it is reached.",
    ) {
        ExampleReadout(label = "Signed in", value = isSignedIn.toString())
        ExampleAction(
            label = if (isSignedIn) {
                "Sign out"
            } else {
                "Sign in"
            },
            onClick = {
                if (isSignedIn) {
                    session.signOut()
                } else {
                    session.signIn()
                }
            },
        )
        ExampleAction(
            label = "Open the secret page",
            onClick = { navigator.push(MembersSecretRoute) },
        )
        ExampleNote(
            text = "Try it signed out: the push never renders the secret page, because the guard " +
                "runs before the stack reaches the host. Then open it signed in and sign out " +
                "while you are there — the page leaves on its own, because the guard announced " +
                "that its answer changed.",
        )
    }
}

@Composable
fun MembersSecretScreen(session: SessionStore) {
    ExampleScaffold(
        title = "Secret page",
        subtitle = "Sign out from here and watch the stack correct itself.",
    ) {
        ExampleAction(
            label = "Sign out",
            onClick = { session.signOut() },
        )
        ExampleNote(
            text = "Nothing navigates when a session ends. The host revalidates its own stack on " +
                "the guard's invalidation, which is how a route that has *become* invalid leaves " +
                "instead of sitting there until something else happens to navigate.",
        )
    }
}

@Composable
fun SignInScreen(
    route: SignInRoute,
    session: SessionStore,
) {
    val navigator = LocalNavigator.current

    ExampleScaffold(
        title = "Sign in",
        subtitle = "The guard put this here, and told it where the user was going.",
    ) {
        ExampleReadout(
            label = "Was heading to",
            value = route.next?.let { next -> next::class.simpleName ?: "a route" } ?: "nowhere",
        )
        ExampleAction(
            label = "Sign in and continue",
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
        ExampleNote(
            text = "The guard redirected by *substitution* — it replaced the secret route where it " +
                "sat rather than handing back the old stack — and carried the original destination " +
                "on this route so nothing about the user's intent was lost.",
        )
    }
}

@Composable
fun VaultLobbyScreen(session: PinSession) {
    val navigator = LocalNavigator.current
    val isLocked by session.locked.collectAsState()

    ExampleScaffold(
        title = "Vault",
        subtitle = "A 401 arrives mid-session; the destination is still right, the session is not.",
    ) {
        ExampleReadout(label = "Session locked", value = isLocked.toString())
        ExampleAction(
            label = "Open the vault",
            onClick = { navigator.push(VaultRoute) },
        )
        ExampleAction(
            label = "Simulate a 401",
            onClick = { session.lock() },
        )
        ExampleNote(
            text = "Lock the session and then open the vault: the guard defers instead of " +
                "redirecting, puts a PIN prompt up, and continues to the vault once the PIN is " +
                "right. The PIN is 1234. Get it wrong and the vault is refused instead.",
        )
    }
}

@Composable
fun VaultScreen(session: PinSession) {
    ExampleScaffold(
        title = "Inside the vault",
        subtitle = "Lock the session from here and watch the guard take the screen away.",
    ) {
        ExampleAction(
            label = "Simulate a 401",
            onClick = { session.lock() },
        )
        ExampleNote(
            text = "The guard's `meanwhile` drops every protected route, so this page is off the " +
                "screen while the PIN is being asked for — and comes back with the same stack " +
                "afterwards, not a rebuilt one.",
        )
    }
}

@Composable
fun PinEntryScreen(session: PinSession) {
    var pin by rememberSaveable { mutableStateOf("") }

    ExampleScaffold(
        title = "Enter your PIN",
        subtitle = "The guard is suspended, waiting for this screen to answer.",
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = { entered -> pin = entered },
            label = { Text(text = "PIN") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleAction(
            label = "Unlock",
            onClick = { session.submit(pin) },
            enabled = pin.isNotBlank(),
        )
        ExampleNote(
            text = "This route is a `TransientRoute`. If the process died right now, the stack " +
                "would be restored without it — otherwise it would come back as a prompt with no " +
                "coroutine left to answer it, and no way out.",
        )
    }
}
