package io.thernal.navkit.sample.shared.deeplinks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.thernal.navkit.navigation.api.presentation.deeplink.DeepLinkIngress
import io.thernal.navkit.sample.shared.app.DeepLinkLog
import io.thernal.navkit.sample.shared.ui.DemoButton
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private val presets = listOf(
    "App scheme" to "navkit://product/42",
    "Web" to "https://example.com/product/42",
    "Other domain" to "https://elsewhere.example/product/42",
)

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

    SampleScreen(
        title = "Link tester",
        topic = Topic.DEEP_LINKS,
        subtitle = "Deliver a link the way the platform would, and watch the stack change.",
        howItWorks = {
            Explanation(
                "Both registered forms reach the same handler: the parser removes whichever " +
                    "registered base a link starts with — `navkit://` or `https://example.com` — " +
                    "and what follows is the page. A feature declares its page once. A link on any " +
                    "other domain starts with no registered base and is not found.",
            )
            Explanation(
                "The platform delivers the same way: `adb shell am start -d navkit://product/7` " +
                    "on Android, `xcrun simctl openurl booted navkit://product/7` on iOS.",
            )
        },
    ) {
        LinkResult(lastResult = lastResult)
        OutlinedTextField(
            value = uri,
            onValueChange = { entered -> uri = entered },
            label = { Text(text = "Link") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (label, preset) ->
                AssistChip(
                    onClick = {
                        uri = preset
                        deliver(ingress = ingress, log = log, uri = uri)
                    },
                    label = { Text(text = label) },
                )
            }
        }
        DemoButton(
            label = "Deliver this link",
            onClick = { deliver(ingress = ingress, log = log, uri = uri) },
        )
    }
}
