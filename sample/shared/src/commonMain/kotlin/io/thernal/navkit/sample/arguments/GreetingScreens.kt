package io.thernal.navkit.sample.arguments

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import io.thernal.navkit.navigation.api.presentation.argument.LocalNavigationArguments
import io.thernal.navkit.navigation.api.presentation.argument.whileInStack
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

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

    ExampleScaffold(
        title = "Set a name",
        subtitle = "Put the argument, then push the screen that reads it.",
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { entered -> name = entered },
            label = { Text(text = "Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleAction(
            label = "Open the reader",
            onClick = {
                // Put and push in the same action. That ordering is the rule: the routes that read
                // the argument are pushed right after it is stored, so the stack change that starts
                // the flow is the one that first sees it alive.
                arguments.put(
                    key = GreetingName,
                    value = name,
                    scope = whileInStack { it is GreetingReaderRoute },
                )
                navigator.push(GreetingReaderRoute)
            },
        )
        ExampleNote(
            text = "The argument's lifetime is a question about the back stack, not a reference " +
                "count. A count released on dispose would hit zero one push early, because " +
                "Navigation3 composes only the entries of the current scene.",
        )
    }
}

@Composable
fun GreetingReaderScreen() {
    val arguments = LocalNavigationArguments.current
    val name = arguments.get(GreetingName)

    ExampleScaffold(
        title = "Reader",
        subtitle = "Nothing was threaded through this route to get the value here.",
    ) {
        ExampleReadout(label = "Name", value = name ?: "nothing — the flow was left")
        ExampleNote(
            text = "Go back and the argument is dropped, because no route matching its scope is " +
                "in the stack any more. Come here again and it is set again by the screen before.",
        )
    }
}
