package io.thernal.navkit.sample.basics

import androidx.compose.runtime.Composable
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleScaffold

/**
 * The whole of simple navigation: a screen pushes another and the other pops.
 *
 * Nothing is injected and nothing is registered. `LocalNavigator` is provided by the mounted host,
 * so a screen reaches it without a constructor parameter and without knowing which host it is in —
 * a nested host provides its own, and the same screen then drives that one instead.
 */
@Composable
fun BasicsHomeScreen() {
    val navigator = LocalNavigator.current
    ExampleScaffold(
        title = "Push and pop",
        subtitle = "The two commands every other example is built out of.",
    ) {
        ExampleNote(
            text = "`push` adds a route; `popBack` removes the top one. The host renders the " +
                "stack the root ViewModel owns, so both commands are reported back to it rather " +
                "than mutating anything the host holds.",
        )
        ExampleAction(
            label = "Open detail A",
            onClick = { navigator.push(BasicsDetailRoute(id = "A")) },
        )
        ExampleAction(
            label = "Open detail B",
            onClick = { navigator.push(BasicsDetailRoute(id = "B")) },
        )
    }
}

@Composable
fun BasicsDetailScreen(route: BasicsDetailRoute) {
    ExampleScaffold(
        title = "Detail ${route.id}",
        subtitle = "The route itself carries the id — no shared state, no store.",
    ) {
        ExampleNote(
            text = "A route is a small immutable value that survives process death. Anything " +
                "bigger than an identifier belongs in a repository, with only its id here.",
        )
    }
}
