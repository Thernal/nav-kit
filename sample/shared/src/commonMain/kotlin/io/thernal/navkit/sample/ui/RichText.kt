package io.thernal.navkit.sample.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle

/** The explanations are written with `code` and *emphasis*; this renders both. */
@Composable
fun rememberRichText(text: String): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    return remember(key1 = text, key2 = codeBackground) {
        richText(
            text = text,
            code = SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
            emphasis = SpanStyle(fontStyle = FontStyle.Italic),
        )
    }
}

private fun richText(
    text: String,
    code: SpanStyle,
    emphasis: SpanStyle,
): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val segment = StringBuilder()
    var isCode = false
    var isEmphasis = false

    fun flush() {
        if (segment.isEmpty()) {
            return
        }
        val style = when {
            isCode -> code
            isEmphasis -> emphasis
            else -> null
        }
        if (style == null) {
            builder.append(segment.toString())
        } else {
            builder.withStyle(style) { append(segment.toString()) }
        }
        segment.clear()
    }

    for (char in text) {
        when {
            char == '`' -> {
                flush()
                isCode = !isCode
            }

            char == '*' && !isCode -> {
                flush()
                isEmphasis = !isEmphasis
            }

            else -> segment.append(char)
        }
    }
    flush()
    return builder.toAnnotatedString()
}
