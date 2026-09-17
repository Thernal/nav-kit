package io.thernal.navkit.sample.deeplinks

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
import io.thernal.navkit.navigation.api.domain.DeepLinkSource
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.navigation.api.presentation.navigator.LocalNavigator
import io.thernal.navkit.sample.app.DeepLinkLog
import io.thernal.navkit.sample.ui.ExampleAction
import io.thernal.navkit.sample.ui.ExampleNote
import io.thernal.navkit.sample.ui.ExampleReadout
import io.thernal.navkit.sample.ui.ExampleScaffold

/**
 * Publishing a link by hand, which is all an activity's `onNewIntent` or an iOS URL callback does.
 * Everything after that is the application's, and none of it is on this screen — except the answer,
 * which the root logs so a link that went nowhere says why.
 */
@Composable
fun LinkPlaygroundScreen(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
) {
    var uri by rememberSaveable { mutableStateOf("navkit://product/42") }
    val lastResult by log.last.collectAsState()

    ExampleScaffold(
        title = "Deep links",
        subtitle = "Deliver a link the way the platform would, and watch the stack change.",
    ) {
        OutlinedTextField(
            value = uri,
            onValueChange = { entered -> uri = entered },
            label = { Text(text = "Link") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExampleReadout(label = "Last result", value = lastResult ?: "nothing delivered yet")
        ExampleAction(
            label = "Deliver this link",
            onClick = { deliver(ingress = ingress, log = log, uri = uri) },
        )
        ExampleAction(
            label = "Try the web form instead",
            onClick = {
                uri = "https://example.com/product/42"
                deliver(ingress = ingress, log = log, uri = uri)
            },
        )
        ExampleNote(
            text = "Both forms reach the same handler. A feature declares its page once and gets " +
                "the app-scheme and the web form for free, because the parser derives the rule " +
                "from the scheme instead of reading a configured list of app schemes. The platform " +
                "delivers the same way: `adb shell am start -d navkit://product/7` on Android, " +
                "`xcrun simctl openurl booted navkit://product/7` on iOS.",
        )
    }
}

@Composable
fun ProductScreen(route: ProductRoute) {
    ExampleScaffold(
        title = "Product ${route.id}",
        subtitle = "Opened by a link, with the catalog and the playground left behind it.",
    ) {
        ExampleNote(
            text = "The handler returned a whole stack, not a destination. That is why back from " +
                "here goes somewhere sensible instead of closing the app.",
        )
    }
}

@Composable
fun LinkCampaignScreen(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
) {
    val lastResult by log.last.collectAsState()

    ExampleScaffold(
        title = "Campaign links",
        subtitle = "Links that land deep, carry a source, and meet a guard.",
    ) {
        ExampleReadout(label = "Last result", value = lastResult ?: "nothing delivered yet")
        ExampleAction(
            label = "navkit://orders — the list",
            onClick = { deliver(ingress = ingress, log = log, uri = "navkit://orders") },
        )
        ExampleAction(
            label = "navkit://orders/77 — three screens deep",
            onClick = { deliver(ingress = ingress, log = log, uri = "navkit://orders/77") },
        )
        ExampleAction(
            label = "The same link from an in-app notification (rejected)",
            onClick = {
                deliver(
                    ingress = ingress,
                    log = log,
                    uri = "navkit://orders/77",
                    source = DeepLinkSource.IN_APP_NOTIFICATION,
                )
            },
        )
        ExampleAction(
            label = "navkit://secret — a guarded destination",
            onClick = {
                deliver(
                    ingress = ingress,
                    log = log,
                    uri = "navkit://secret",
                    source = DeepLinkSource.PUSH_NOTIFICATION,
                )
            },
        )
        ExampleNote(
            text = "The rejected one stays on this screen and says why: the ingress accepted it, " +
                "the handler refused its source. The last one is the interesting case. Signed " +
                "out, the link resolves to the secret page and the guard rewrites it to a sign-in " +
                "screen before anything renders — and the root that applied the link never had " +
                "to ask.",
        )
    }
}

@Composable
fun OrdersScreen() {
    val navigator = LocalNavigator.current
    ExampleScaffold(
        title = "Orders",
        subtitle = "The middle of a link-built stack.",
    ) {
        ExampleAction(
            label = "Open order 77",
            onClick = { navigator.push(OrderRoute(id = "77")) },
        )
    }
}

@Composable
fun OrderScreen(route: OrderRoute) {
    ExampleScaffold(
        title = "Order ${route.id}",
        subtitle = "Reached by a link or by a push — the screen cannot tell, and should not.",
    ) {
        ExampleNote(
            text = "A handler decides what a link means; a screen only renders its route. That " +
                "split is what keeps a deep link from becoming a second navigation system.",
        )
    }
}

/**
 * Hands [uri] to the ingress. What a handler makes of it arrives later, through [log]; only a link
 * the ingress itself turns away is recorded here.
 */
private fun deliver(
    ingress: DeepLinkIngress,
    log: DeepLinkLog,
    uri: String,
    source: DeepLinkSource = DeepLinkSource.EXTERNAL_LINK,
) {
    if (!ingress.publish(uri = uri, source = source)) {
        log.recordRefused(uri)
    }
}
