package com.example.ncmconverter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ncmconverter.R

private val RhrExtraLight = Font(R.font.resourcehanroundsc_extralight, FontWeight(200))
private val RhrLight = Font(R.font.resourcehanroundsc_light, FontWeight(300))
private val RhrRegular = Font(R.font.resourcehanroundsc_regular, FontWeight(400))
private val RhrMedium = Font(R.font.resourcehanroundsc_medium, FontWeight(500))
private val RhrBold = Font(R.font.resourcehanroundsc_bold, FontWeight(700))
private val RhrHeavy = Font(R.font.resourcehanroundsc_heavy, FontWeight(900))

val RhrFontFamily = FontFamily(RhrExtraLight, RhrLight, RhrRegular, RhrMedium, RhrBold, RhrHeavy)

val AppFontWeights = listOf(
    FontWeight(200) to "ExtraLight",
    FontWeight(300) to "Light",
    FontWeight(400) to "Regular",
    FontWeight(500) to "Medium",
    FontWeight(700) to "Bold",
    FontWeight(900) to "Heavy",
)

fun appTypography(fontWeight: FontWeight): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        displayMedium = base.displayMedium.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        displaySmall = base.displaySmall.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        headlineLarge = base.headlineLarge.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        headlineMedium = base.headlineMedium.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        headlineSmall = base.headlineSmall.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        titleLarge = base.titleLarge.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        titleMedium = base.titleMedium.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        titleSmall = base.titleSmall.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        bodyLarge = base.bodyLarge.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        bodyMedium = base.bodyMedium.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        bodySmall = base.bodySmall.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        labelLarge = base.labelLarge.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        labelMedium = base.labelMedium.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
        labelSmall = base.labelSmall.copy(fontFamily = RhrFontFamily, fontWeight = fontWeight),
    )
}
