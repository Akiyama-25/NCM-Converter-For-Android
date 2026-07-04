package com.example.ncmconverter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Error = Color(0xFFCF6679)
val ErrorLight = Color(0xFFB3261E)
val Success = Color(0xFF4CAF50)

val DefaultDarkScheme = darkColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color(0xFF121212),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFA0A0A0),
    error = Error
)

val DefaultLightScheme = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color.White,
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    error = ErrorLight
)

private fun contrastOn(color: Color): Color {
    return if (color.luminance() > 0.5f) Color.Black else Color.White
}

private fun blend(fg: Color, bg: Color, fraction: Float): Color {
    val r = fg.red + (bg.red - fg.red) * fraction
    val g = fg.green + (bg.green - fg.green) * fraction
    val b = fg.blue + (bg.blue - fg.blue) * fraction
    val a = fg.alpha + (bg.alpha - fg.alpha) * fraction
    return Color(r, g, b, a)
}

private fun lighten(color: Color, fraction: Float): Color = blend(color, Color.White, fraction)
private fun darken(color: Color, fraction: Float): Color = blend(color, Color.Black, fraction)

fun ColorScheme.applyUserColors(
    darkTheme: Boolean,
    accent: Color,
    bg: Color,
    monetEnabled: Boolean
): ColorScheme {
    val onSurfaceVariant = if (darkTheme) Color(0xFFA0A0A0) else Color(0xFF666666)
    val onSurface = if (darkTheme) Color(0xFFE0E0E0) else Color(0xFF1C1B1F)
    val outline = if (darkTheme) Color(0xFF444444) else Color(0xFFCCCCCC)
    val outlineVariant = if (darkTheme) Color(0xFF333333) else Color(0xFFDDDDDD)

    return if (monetEnabled) {
        // Monet ON: keep wallpaper-derived primary & surface colors intact,
        // only fix contrast/accessibility text colors
        copy(
            onSurfaceVariant = onSurfaceVariant,
            onSurface = onSurface,
            onBackground = onSurface,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        // Monet OFF: apply user's custom accent & background colors
        val onPrimary = contrastOn(accent)
        val primaryContainer = lighten(accent, 0.8f)
        val onPrimaryContainer = darken(accent, 0.3f)
        val surfaceVariant = if (darkTheme) darken(bg, 0.05f) else lighten(bg, 0.05f)

        copy(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            background = bg,
            onBackground = onSurface,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }
}
