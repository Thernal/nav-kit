package io.thernal.navkit.sample.shared.guards

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.thernal.navkit.sample.shared.ui.Explanation
import io.thernal.navkit.sample.shared.ui.IconBadge
import io.thernal.navkit.sample.shared.ui.SampleScreen
import io.thernal.navkit.sample.shared.ui.Topic

private const val PIN_LENGTH = 4

private val accent = Topic.GUARDS.accent

private val keypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", "⌫"),
)

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
