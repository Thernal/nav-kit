package io.thernal.navkit.sample.shared.ui

import androidx.compose.ui.graphics.Color

/** The capability an example belongs to — its catalog section, its icon and its accent colour. */
enum class Topic(
    val label: String,
    val emoji: String,
    val accent: Color,
) {
    NAVIGATION("Navigation", "🧭", SamplePalette.Indigo),
    ARGUMENTS("Arguments", "📦", SamplePalette.Teal),
    RESULTS("Results", "↩️", SamplePalette.Pink),
    GUARDS("Guards", "🛡️", SamplePalette.Amber),
    BACK_HANDLING("Back handling", "⏪", SamplePalette.Rose),
    DEEP_LINKS("Deep links", "🔗", SamplePalette.Sky),
    NESTED_NAVIGATION("Nested navigation", "🗂️", SamplePalette.Green),
    BOTTOM_SHEETS("Bottom sheets", "🧾", SamplePalette.Violet),
}
