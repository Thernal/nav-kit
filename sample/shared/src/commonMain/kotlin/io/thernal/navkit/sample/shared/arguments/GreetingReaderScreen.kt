package io.thernal.navkit.sample.shared.arguments

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.HeroCard
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val accent = Topic.ARGUMENTS.accent

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
