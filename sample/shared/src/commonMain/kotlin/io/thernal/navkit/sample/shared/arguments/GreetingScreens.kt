package io.thernal.navkit.sample.arguments

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.BottomAction
import io.thernal.navkit.sample.ui.Explanation
import io.thernal.navkit.sample.ui.HeroCard
import io.thernal.navkit.sample.ui.SampleScreen
import io.thernal.navkit.sample.ui.SectionLabel
import io.thernal.navkit.sample.ui.Topic

private val accent = Topic.ARGUMENTS.accent

/**
 * A value travelling **forwards**: set by a screen that is opening another, read by a screen that
 * does not exist yet.
 *
 * The opposite direction from a result, and the reason the two are separate mechanisms. Using a
 * results mailbox for this would mean posting a value for a consumer that has not been created, and
 * hoping it is still there when one is.
 */
@Composable
fun GreetingSetupScreen() {
    val navigator = LocalNavigator.current
    val arguments = LocalNavigationArguments.current
    var name by rememberSaveable { mutableStateOf("Ada") }

    SampleScreen(
        title = "New card",
        topic = Topic.ARGUMENTS,
        bottomBar = {
            BottomAction(
                label = "Send the card",
                color = accent,
                enabled = name.isNotBlank(),
                onClick = {
                    // Put and push in the same action. That ordering is the rule: the routes that
                    // read the argument are pushed right after it is stored, so the stack change
                    // that starts the flow is the one that first sees it alive.
                    arguments.put(
                        key = GreetingName,
                        value = name,
                        scope = whileInStack { it is GreetingReaderRoute },
                    )
                    navigator.push(GreetingReaderRoute)
                },
            )
        },
        howItWorks = {
            Explanation(
                "Sending puts the name as an argument scoped to the card's route, then pushes the " +
                    "card — the name travels without being threaded through the route.",
            )
            Explanation(
                "The argument's lifetime is a question about the back stack, not a reference " +
                    "count. A count released on dispose would hit zero one push early, because " +
                    "Navigation3 composes only the entries of the current scene.",
            )
        },
    ) {
        SectionLabel(text = "To")
        OutlinedTextField(
            value = name,
            onValueChange = { entered -> name = entered },
            label = { Text(text = "Recipient's name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel(text = "Preview")
        HeroCard(
            title = "Hello, ${name.ifBlank { "…" }}!",
            emoji = "💌",
            accent = accent,
            subtitle = "Thinking of you today.",
        )
    }
}

@Composable
fun GreetingReaderScreen() {
    val arguments = LocalNavigationArguments.current
    val name = arguments.get(GreetingName)

    SampleScreen(
        title = "Your card",
        topic = Topic.ARGUMENTS,
        howItWorks = {
            Explanation(
                "Nothing was threaded through this route to get the name here: the card reads " +
                    "`arguments.get(GreetingName)`. Go back and the argument is dropped, because no " +
                    "route matching its scope is in the stack any more.",
            )
        },
    ) {
        if (name == null) {
            HeroCard(
                title = "This card is empty",
                emoji = "📭",
                accent = accent,
                subtitle = "The flow was left, so its argument was dropped.",
            )
        } else {
            HeroCard(
                title = "Hello, $name!",
                emoji = "🎉",
                accent = accent,
                subtitle = "Thinking of you today. See you soon.",
            )
        }
    }
}
