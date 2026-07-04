package com.example.ncmconverter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.ncmconverter.util.AppPrefs

@Composable
fun NcmConverterTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val themeMode by AppPrefs.themeFlow.collectAsState()
    val useMonet by AppPrefs.monetFlow.collectAsState()
    val accentColor by AppPrefs.accentColorFlow.collectAsState()
    val lightBg by AppPrefs.lightBgColorFlow.collectAsState()
    val darkBg by AppPrefs.darkBgColorFlow.collectAsState()

    val darkTheme = when (themeMode) {
        AppPrefs.THEME_DARK -> true
        AppPrefs.THEME_LIGHT -> false
        else -> {
            val nightMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    val bg = Color(if (darkTheme) darkBg.toInt() else lightBg.toInt())
    val accent = Color(accentColor.toInt())

    // Step 1: Resolve base scheme (dynamic or default)
    val baseScheme = if (useMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DefaultDarkScheme else DefaultLightScheme
    }

    // Step 2: Apply user custom accent & background colors
    val colorScheme = baseScheme.applyUserColors(darkTheme, accent, bg, useMonet)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
