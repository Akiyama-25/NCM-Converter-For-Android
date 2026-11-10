package com.example.ncmconverter.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val Error = Color(0xFFCF6679)
val ErrorLight = Color(0xFFB3261E)
val Success = Color(0xFF2EBD59)

val DefaultDarkScheme = darkColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color(0xFF0A1F11),
    primaryContainer = Color(0xFF144225),
    onPrimaryContainer = Color(0xFFA7F3C1),
    secondary = Color(0xFF66D491),
    secondaryContainer = Color(0xFF1B3525),
    onSecondaryContainer = Color(0xFFC7F7D8),
    surface = Color(0xFF111315),
    onSurface = Color(0xFFE6E8EA),
    surfaceVariant = Color(0xFF1D2024),
    onSurfaceVariant = Color(0xFF9EA4AE),
    surfaceContainerLowest = Color(0xFF0B0D0E),
    surfaceContainerLow = Color(0xFF16181B),
    surfaceContainer = Color(0xFF1B1E22),
    surfaceContainerHigh = Color(0xFF23272C),
    surfaceContainerHighest = Color(0xFF2D3138),
    error = Error
)

val DefaultLightScheme = lightColorScheme(
    primary = Color(0xFF169C46),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F7E1),
    onPrimaryContainer = Color(0xFF073818),
    secondary = Color(0xFF2B6B43),
    secondaryContainer = Color(0xFFE4F5EA),
    onSecondaryContainer = Color(0xFF12361F),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF181C1E),
    surfaceVariant = Color(0xFFECEEF1),
    onSurfaceVariant = Color(0xFF5A6065),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F6),
    surfaceContainer = Color(0xFFECEEF0),
    surfaceContainerHigh = Color(0xFFE5E8EB),
    surfaceContainerHighest = Color(0xFFDEE2E6),
    error = ErrorLight
)

private fun contrastOn(color: Color): Color {
    return if (color.luminance() > 0.5f) Color(0xFF121416) else Color.White
}

private fun blend(fg: Color, bg: Color, fraction: Float): Color {
    val r = fg.red + (bg.red - fg.red) * fraction
    val g = fg.green + (bg.green - fg.green) * fraction
    val b = fg.blue + (bg.blue - fg.blue) * fraction
    val a = fg.alpha + (bg.alpha - fg.alpha) * fraction
    return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
}

private fun lighten(color: Color, fraction: Float): Color = blend(color, Color.White, fraction)
private fun darken(color: Color, fraction: Float): Color = blend(color, Color.Black, fraction)

/**
 * M3E (Material 3 Expressive) & FlClash-inspired surface container and accent color synthesizer.
 */
fun ColorScheme.applyUserColors(
    darkTheme: Boolean,
    accent: Color,
    bg: Color,
    monetEnabled: Boolean
): ColorScheme {
    val onSurfaceVariant = if (darkTheme) Color(0xFFA2A8B2) else Color(0xFF5A6169)
    val onSurface = if (darkTheme) Color(0xFFE8EAED) else Color(0xFF1A1C1E)
    val outline = if (darkTheme) Color(0xFF42474E) else Color(0xFFC4C7CE)
    val outlineVariant = if (darkTheme) Color(0xFF2B2F35) else Color(0xFFE1E3E8)

    return if (monetEnabled) {
        copy(
            onSurfaceVariant = onSurfaceVariant,
            onSurface = onSurface,
            onBackground = onSurface,
            outline = outline,
            outlineVariant = outlineVariant
        )
    } else {
        val onPrimary = contrastOn(accent)
        val primaryContainer = if (darkTheme) blend(accent, bg, 0.72f) else lighten(accent, 0.82f)
        val onPrimaryContainer = if (darkTheme) lighten(accent, 0.65f) else darken(accent, 0.45f)
        val secondaryContainer = if (darkTheme) blend(accent, bg, 0.82f) else lighten(accent, 0.88f)
        val onSecondaryContainer = if (darkTheme) lighten(accent, 0.72f) else darken(accent, 0.40f)

        val surfaceContainerLowest = if (darkTheme) darken(bg, 0.20f) else Color.White
        val surfaceContainerLow = if (darkTheme) lighten(bg, 0.03f) else darken(bg, 0.02f)
        val surfaceContainer = if (darkTheme) lighten(bg, 0.06f) else darken(bg, 0.045f)
        val surfaceContainerHigh = if (darkTheme) lighten(bg, 0.10f) else darken(bg, 0.07f)
        val surfaceContainerHighest = if (darkTheme) lighten(bg, 0.14f) else darken(bg, 0.10f)
        val surfaceVariant = if (darkTheme) blend(accent, surfaceContainerHigh, 0.90f) else blend(accent, surfaceContainerHigh, 0.92f)

        copy(
            primary = accent,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = accent,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            surface = bg,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            background = bg,
            onBackground = onSurface,
            outline = outline,
            outlineVariant = outlineVariant
        )
    }
}
