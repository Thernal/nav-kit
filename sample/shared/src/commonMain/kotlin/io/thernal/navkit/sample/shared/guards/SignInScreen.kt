package io.thernal.navkit.sample.shared.guards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.thernal.navkit.navigation.api.presentation.model.Route
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.LiveValue
import io.thernal.navkit.sample.shared.ui.PrimaryButton
import io.thernal.navkit.sample.shared.ui.SamplePalette
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.GUARDS.accent

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
            accent = SamplePalette.Indigo,
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
