package io.thernal.navkit.sample.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val Indigo = Color(0xFF5B5BD6)
internal val Pink = Color(0xFFD6409F)
internal val Teal = Color(0xFF12A594)
internal val Amber = Color(0xFFE5A000)
internal val Rose = Color(0xFFE5484D)
internal val Sky = Color(0xFF0090FF)
internal val Green = Color(0xFF30A46C)

private val IndigoDeep = Color(0xFF2F2F8F)
private val IndigoSoft = Color(0xFFE6E6FB)
private val IndigoPale = Color(0xFFB8B8F5)
private val TealSoft = Color(0xFFD5F4EF)
private val TealDeep = Color(0xFF0B5D53)
private val AmberSoft = Color(0xFFFFF1CC)
private val AmberDeep = Color(0xFF6B4B00)
private val RoseSoft = Color(0xFFFFE3E3)
private val RoseDeep = Color(0xFF8C1D21)

private val LightBackground = Color(0xFFF6F6FA)
private val LightSurfaceVariant = Color(0xFFECECF3)
private val LightOnSurface = Color(0xFF1B1B26)
private val LightOnSurfaceVariant = Color(0xFF5E5E72)
private val LightOutline = Color(0xFFD4D4E0)

private val DarkBackground = Color(0xFF111118)
private val DarkSurface = Color(0xFF1A1A24)
private val DarkSurfaceVariant = Color(0xFF262633)
private val DarkOnSurface = Color(0xFFECECF4)
private val DarkOnSurfaceVariant = Color(0xFFA6A6BA)
private val DarkOutline = Color(0xFF3A3A4A)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    primaryContainer = IndigoSoft,
    onPrimaryContainer = IndigoDeep,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealSoft,
    onSecondaryContainer = TealDeep,
    tertiary = Amber,
    onTertiary = Color.White,
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = AmberDeep,
    error = Rose,
    errorContainer = RoseSoft,
    onErrorContainer = RoseDeep,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = Color.White,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color.White,
    surfaceContainerHigh = LightSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
)

private val DarkColors = darkColorScheme(
    primary = IndigoPale,
    onPrimary = IndigoDeep,
    primaryContainer = IndigoDeep,
    onPrimaryContainer = IndigoSoft,
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealDeep,
    onSecondaryContainer = TealSoft,
    tertiary = Amber,
    onTertiary = AmberDeep,
    tertiaryContainer = AmberDeep,
    onTertiaryContainer = AmberSoft,
    error = Rose,
    errorContainer = RoseDeep,
    onErrorContainer = RoseSoft,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
)

private val SampleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val SampleTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Material 3 with the sample's own palette, light and dark. */
@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        DarkColors
    } else {
        LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SampleTypography,
        shapes = SampleShapes,
        content = content,
    )
}
