package io.thernal.navkit.sample.ui

import androidx.compose.ui.graphics.Color

/** The capability an example belongs to — its catalog section, its icon and its accent colour. */
enum class Topic(
    val label: String,
    val emoji: String,
    val accent: Color,
) {
    NAVIGATION("Navigation", "🧭", Indigo),
    ARGUMENTS("Arguments", "📦", Teal),
    RESULTS("Results", "↩️", Pink),
    GUARDS("Guards", "🛡️", Amber),
    BACK_HANDLING("Back handling", "⏪", Rose),
    DEEP_LINKS("Deep links", "🔗", Sky),
    NESTED_NAVIGATION("Nested navigation", "🗂️", Green),
}
