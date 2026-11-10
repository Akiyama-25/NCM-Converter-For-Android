package com.example.ncmconverter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppFontWeights = listOf(
    FontWeight(100) to "Thin (100)",
    FontWeight(200) to "ExtraLight (200)",
    FontWeight(300) to "Light (300)",
    FontWeight(400) to "Regular (400)",
    FontWeight(500) to "Medium (500)",
    FontWeight(600) to "SemiBold (600)",
    FontWeight(700) to "Bold (700)",
    FontWeight(800) to "ExtraBold (800)",
    FontWeight(900) to "Black (900)",
)

/**
 * Material 3 Expressive (M3E) typography generator using dynamically selected system-installed
 * font families and stepped expressive font weight hierarchy (inspired by FlClash).
 */
fun appTypography(
    fontFamily: FontFamily = FontFamily.Default,
    baseWeight: FontWeight = FontWeight.Normal
): Typography {
    val base = Typography()
    val w = baseWeight.weight.coerceIn(100, 900)
    val bodyW = FontWeight(w)
    val mediumW = FontWeight((w + 100).coerceIn(100, 900))
    val strongW = FontWeight((w + 200).coerceIn(100, 900))

    return Typography(
        displayLarge = base.displayLarge.copy(
            fontFamily = fontFamily,
            fontWeight = strongW,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = base.displayMedium.copy(
            fontFamily = fontFamily,
            fontWeight = strongW,
            letterSpacing = (-0.25).sp
        ),
        displaySmall = base.displaySmall.copy(
            fontFamily = fontFamily,
            fontWeight = strongW
        ),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = fontFamily,
            fontWeight = strongW,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = fontFamily,
            fontWeight = strongW
        ),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = fontFamily,
            fontWeight = mediumW
        ),
        titleLarge = base.titleLarge.copy(
            fontFamily = fontFamily,
            fontWeight = strongW
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = fontFamily,
            fontWeight = mediumW,
            letterSpacing = 0.1.sp
        ),
        titleSmall = base.titleSmall.copy(
            fontFamily = fontFamily,
            fontWeight = mediumW
        ),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = fontFamily,
            fontWeight = bodyW
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = fontFamily,
            fontWeight = bodyW
        ),
        bodySmall = base.bodySmall.copy(
            fontFamily = fontFamily,
            fontWeight = bodyW
        ),
        labelLarge = base.labelLarge.copy(
            fontFamily = fontFamily,
            fontWeight = mediumW
        ),
        labelMedium = base.labelMedium.copy(
            fontFamily = fontFamily,
            fontWeight = mediumW
        ),
        labelSmall = base.labelSmall.copy(
            fontFamily = fontFamily,
            fontWeight = bodyW
        ),
    )
}
