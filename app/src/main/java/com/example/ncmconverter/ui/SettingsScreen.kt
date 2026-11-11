package com.example.ncmconverter.ui

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.ncmconverter.R
import com.example.ncmconverter.ui.theme.AppFontWeights
import com.example.ncmconverter.ui.theme.SystemFontEntry
import com.example.ncmconverter.ui.theme.SystemFontScanner
import com.example.ncmconverter.ui.theme.colorToHsl
import com.example.ncmconverter.ui.theme.hslToArgbLong
import com.example.ncmconverter.ui.theme.hslToColor
import com.example.ncmconverter.util.AppLogger
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ColorOption(@StringRes val labelRes: Int, val color: Color)

private val AccentColors = listOf(
    ColorOption(R.string.color_green, Color(0xFF1DB954)),
    ColorOption(R.string.color_blue, Color(0xFF1976D2)),
    ColorOption(R.string.color_purple, Color(0xFF7B1FA2)),
    ColorOption(R.string.color_red, Color(0xFFD32F2F)),
    ColorOption(R.string.color_orange, Color(0xFFF57C00)),
    ColorOption(R.string.color_cyan, Color(0xFF00838F)),
    ColorOption(R.string.color_pink, Color(0xFFC2185B)),
    ColorOption(R.string.color_indigo, Color(0xFF303F9F)),
)

private val LightBgColors = listOf(
    ColorOption(R.string.color_white, Color(0xFFFAFAFA)),
    ColorOption(R.string.color_light_gray, Color(0xFFF5F5F5)),
    ColorOption(R.string.color_cream, Color(0xFFFFF8E1)),
    ColorOption(R.string.color_light_blue, Color(0xFFE3F2FD)),
)

private val DarkBgColors = listOf(
    ColorOption(R.string.color_black, Color(0xFF121212)),
    ColorOption(R.string.color_dark_gray, Color(0xFF1E1E1E)),
    ColorOption(R.string.color_dark_blue, Color(0xFF0D1B2A)),
    ColorOption(R.string.color_dark_purple, Color(0xFF1A1A2E)),
)

private enum class HslTarget { ACCENT, BG }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onPickOutputFolder: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = context as Activity

    var themeMode by remember { mutableStateOf(AppPrefs.themeMode) }
    var autoSave by remember { mutableStateOf(AppPrefs.autoSave) }
    var enableLyric by remember { mutableStateOf(AppPrefs.enableLyric) }
    var lyricEmbedOriginal by remember { mutableStateOf(AppPrefs.lyricEmbedOriginal) }
    var lyricEmbedTranslated by remember { mutableStateOf(AppPrefs.lyricEmbedTranslated) }
    var enableCover by remember { mutableStateOf(AppPrefs.enableCover) }
    var useMonet by remember { mutableStateOf(AppPrefs.useMonetColors) }
    var gridColumns by remember { mutableIntStateOf(AppPrefs.gridColumns) }
    val appliedGridColumns by AppPrefs.gridColumnsFlow.collectAsState()
    var portraitGridEnabled by remember { mutableStateOf(AppPrefs.portraitGridEnabled) }
    var accentColor by remember { mutableLongStateOf(AppPrefs.customAccentColor) }
    var lightBgColor by remember { mutableLongStateOf(AppPrefs.customLightBgColor) }
    var darkBgColor by remember { mutableLongStateOf(AppPrefs.customDarkBgColor) }
    var appLanguage by remember { mutableStateOf(AppPrefs.appLanguage) }
    val systemFontId by AppPrefs.systemFontIdFlow.collectAsState()
    var fontWeight by remember { mutableIntStateOf(AppPrefs.fontWeight) }

    var systemFonts by remember { mutableStateOf(SystemFontScanner.getLogicalPresets()) }
    var showFontPickerSheet by remember { mutableStateOf(false) }
    var fontSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val scanned = SystemFontScanner.scanSystemFonts()
            withContext(Dispatchers.Main) {
                systemFonts = scanned
            }
        }
    }

    val isDark = when (themeMode) {
        AppPrefs.THEME_DARK -> true
        AppPrefs.THEME_LIGHT -> false
        else -> {
            val nightMode = context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK
            nightMode == Configuration.UI_MODE_NIGHT_YES
        }
    }

    var showInstructionsDialog by remember { mutableStateOf(false) }
    var showColumnsDialog by remember { mutableStateOf(false) }
    var pendingColumns by remember { mutableIntStateOf(gridColumns) }
    var showLanguageChangeDialog by remember { mutableStateOf(false) }
    var pendingLanguageCode by remember { mutableStateOf("") }

    var showHslSheet by remember { mutableStateOf(false) }
    var hslTarget by remember { mutableStateOf(HslTarget.ACCENT) }
    var sliderH by remember { mutableFloatStateOf(0f) }
    var sliderS by remember { mutableFloatStateOf(0f) }
    var sliderL by remember { mutableFloatStateOf(0f) }

    // Debug & Logs state
    var logLevel by remember { mutableStateOf(AppPrefs.logLevel) }
    var logFormat by remember { mutableStateOf(AppPrefs.logFormat) }
    var showLogDialog by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }

    LaunchedEffect(showLogDialog, logLevel, logFormat) {
        if (showLogDialog) {
            withContext(Dispatchers.IO) {
                logText = AppLogger.getLogContent(context, logLevel, logFormat)
            }
        }
    }

    // Determine output folder display name
    val outputFolderName = remember(AppPrefs.customOutputUri) {
        if (AppPrefs.customOutputUri.isNotBlank()) {
            try {
                val treeUri = android.net.Uri.parse(AppPrefs.customOutputUri)
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                doc?.name ?: AppPrefs.customOutputUri
            } catch (_: Exception) {
                AppPrefs.customOutputUri
            }
        } else {
            AppPrefs.customPath
        }
    }

    val isSimplifiedChinese = remember(appLanguage) {
        when (appLanguage) {
            "zh-SC" -> true
            "system" -> {
                val locale = java.util.Locale.getDefault()
                locale.language == "zh" && (locale.country == "CN" || locale.script == "Hans")
            }
            else -> false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .testTag("settings_back_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        val settingsCols = if (appliedGridColumns <= 1) 1
        else when (calculateWindowSizeClass(activity).widthSizeClass) {
            WindowWidthSizeClass.Compact -> 1
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(settingsCols),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // ── Display & M3E Theme ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.section_display))
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.pref_theme_mode),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdaptiveControlsRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 6.dp,
                        verticalSpacing = 6.dp
                    ) {
                        ThemeChip(stringResource(R.string.pref_theme_system), themeMode == AppPrefs.THEME_SYSTEM, isDark) {
                            themeMode = AppPrefs.THEME_SYSTEM; AppPrefs.themeMode = themeMode
                        }
                        ThemeChip(stringResource(R.string.pref_theme_light), themeMode == AppPrefs.THEME_LIGHT, isDark) {
                            themeMode = AppPrefs.THEME_LIGHT; AppPrefs.themeMode = themeMode
                        }
                        ThemeChip(stringResource(R.string.pref_theme_dark), themeMode == AppPrefs.THEME_DARK, isDark) {
                            themeMode = AppPrefs.THEME_DARK; AppPrefs.themeMode = themeMode
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.pref_language),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LanguageDropdown(appLanguage, isDark) { code ->
                        if (code != appLanguage) {
                            appLanguage = code
                            AppPrefs.appLanguage = code
                            pendingLanguageCode = code
                            showLanguageChangeDialog = true
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.pref_grid_columns),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Slider(
                            value = gridColumns.toFloat(),
                            onValueChange = { gridColumns = it.toInt() },
                            onValueChangeFinished = {
                                if (gridColumns == 1 && appliedGridColumns > 1) {
                                    pendingColumns = 1
                                    showColumnsDialog = true
                                } else {
                                    AppPrefs.gridColumns = gridColumns
                                }
                            },
                            valueRange = 1f..5f,
                            steps = 3,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.pref_grid_columns_value, gridColumns),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item(key = "portrait_grid") {
                AnimatedVisibility(
                    visible = appliedGridColumns > 1,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    SettingSwitch(
                        title = stringResource(R.string.pref_portrait_grid),
                        subtitle = stringResource(
                            if (portraitGridEnabled) R.string.pref_portrait_grid_on else R.string.pref_portrait_grid_off
                        ),
                        checked = portraitGridEnabled,
                        onCheckedChange = {
                            portraitGridEnabled = it
                            AppPrefs.portraitGridEnabled = it
                        }
                    )
                }
            }

            // ── FlClash-Style System Installed Font Selector & Weight Control ──
            item(key = "system_font_card", span = { GridItemSpan(maxLineSpan) }) {
                SystemFontSettingCard(
                    selectedFontId = systemFontId,
                    fontWeight = fontWeight,
                    scannedCount = systemFonts.size,
                    onOpenPicker = { showFontPickerSheet = true },
                    onWeightChanged = { newW ->
                        fontWeight = newW
                        AppPrefs.fontWeight = newW
                    },
                    onResetDefault = {
                        AppPrefs.systemFontId = SystemFontScanner.ID_SYSTEM_DEFAULT
                        fontWeight = 400
                        AppPrefs.fontWeight = 400
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    val currentColorScheme = MaterialTheme.colorScheme
                    SettingSwitch(
                        title = stringResource(R.string.pref_monet),
                        subtitle = stringResource(if (useMonet) R.string.pref_monet_on else R.string.pref_monet_off),
                        checked = useMonet,
                        onCheckedChange = { newValue ->
                            if (!newValue) {
                                val accentHsl = colorToHsl(currentColorScheme.primary)
                                val bgHsl = colorToHsl(currentColorScheme.surface)
                                AppPrefs.customAccentH = accentHsl.h
                                AppPrefs.customAccentS = accentHsl.s
                                AppPrefs.customAccentL = accentHsl.l
                                AppPrefs.customBgH = bgHsl.h
                                AppPrefs.customBgS = bgHsl.s
                                AppPrefs.customBgL = bgHsl.l
                                accentColor = currentColorScheme.primary.toArgb().toLong()
                                AppPrefs.customAccentColor = accentColor
                                if (isDark) {
                                    darkBgColor = currentColorScheme.surface.toArgb().toLong()
                                    AppPrefs.customDarkBgColor = darkBgColor
                                } else {
                                    lightBgColor = currentColorScheme.surface.toArgb().toLong()
                                    AppPrefs.customLightBgColor = lightBgColor
                                }
                            }
                            useMonet = newValue
                            AppPrefs.useMonetColors = newValue
                        }
                    )
                }
            }

            item(key = "color_pickers", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = !useMonet,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column {
                        Text(
                            stringResource(R.string.pref_accent_color),
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ColorPickerRow(
                            colors = AccentColors,
                            selectedColor = Color(accentColor.toInt()),
                            onColorSelected = {
                                accentColor = it.toArgb().toLong()
                                AppPrefs.customAccentColor = accentColor
                                val hsl = colorToHsl(it)
                                AppPrefs.customAccentH = hsl.h
                                AppPrefs.customAccentS = hsl.s
                                AppPrefs.customAccentL = hsl.l
                            },
                            onCustomClick = {
                                val hsl = colorToHsl(Color(accentColor.toInt()))
                                sliderH = hsl.h; sliderS = hsl.s; sliderL = hsl.l
                                hslTarget = HslTarget.ACCENT
                                showHslSheet = true
                            }
                        )
                        Text(
                            stringResource(R.string.pref_bg_color),
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ColorPickerRow(
                            colors = if (isDark) DarkBgColors else LightBgColors,
                            selectedColor = Color(if (isDark) darkBgColor else lightBgColor),
                            onColorSelected = {
                                val longVal = it.toArgb().toLong()
                                if (isDark) {
                                    darkBgColor = longVal; AppPrefs.customDarkBgColor = longVal
                                } else {
                                    lightBgColor = longVal; AppPrefs.customLightBgColor = longVal
                                }
                                val hsl = colorToHsl(it)
                                AppPrefs.customBgH = hsl.h
                                AppPrefs.customBgS = hsl.s
                                AppPrefs.customBgL = hsl.l
                            },
                            onCustomClick = {
                                val bgArgb = if (isDark) darkBgColor else lightBgColor
                                val hsl = colorToHsl(Color(bgArgb.toInt()))
                                sliderH = hsl.h; sliderS = hsl.s; sliderL = hsl.l
                                hslTarget = HslTarget.BG
                                showHslSheet = true
                            }
                        )
                    }
                }
            }

            // ── Conversion ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.section_convert))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickOutputFolder() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.pref_save_path),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            outputFolderName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                SettingSwitch(
                    title = stringResource(R.string.pref_auto_save),
                    subtitle = stringResource(if (autoSave) R.string.pref_auto_save_on else R.string.pref_auto_save_off),
                    checked = autoSave,
                    onCheckedChange = { autoSave = it; AppPrefs.autoSave = it }
                )
            }

            item {
                SettingSwitch(
                    title = stringResource(R.string.pref_auto_cover),
                    subtitle = stringResource(if (enableCover) R.string.pref_auto_cover_on else R.string.pref_auto_cover_off),
                    checked = enableCover,
                    onCheckedChange = { enableCover = it; AppPrefs.enableCover = it }
                )
            }

            // ── Lyrics ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.section_lyrics))
            }

            item {
                SettingSwitch(
                    title = stringResource(R.string.pref_auto_lyric),
                    subtitle = stringResource(if (enableLyric) R.string.pref_auto_lyric_on else R.string.pref_auto_lyric_off),
                    checked = enableLyric,
                    onCheckedChange = { enableLyric = it; AppPrefs.enableLyric = it }
                )
            }

            item {
                AnimatedVisibility(
                    visible = enableLyric,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    SettingSwitch(
                        title = stringResource(R.string.pref_lyric_original),
                        subtitle = stringResource(
                            if (lyricEmbedOriginal) R.string.pref_lyric_original_on else R.string.pref_lyric_original_off
                        ),
                        checked = lyricEmbedOriginal,
                        onCheckedChange = { lyricEmbedOriginal = it; AppPrefs.lyricEmbedOriginal = it }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = enableLyric,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    val translatedSubtitle = if (lyricEmbedTranslated) {
                        if (isSimplifiedChinese) {
                            stringResource(R.string.pref_lyric_translated_on)
                        } else {
                            stringResource(R.string.pref_lyric_translated_on) + " " +
                                stringResource(R.string.pref_lyric_translated_note)
                        }
                    } else {
                        stringResource(R.string.pref_lyric_translated_off)
                    }
                    SettingSwitch(
                        title = stringResource(R.string.pref_lyric_translated),
                        subtitle = translatedSubtitle,
                        checked = lyricEmbedTranslated,
                        onCheckedChange = { lyricEmbedTranslated = it; AppPrefs.lyricEmbedTranslated = it }
                    )
                }
            }

            // ── Debug ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.section_debug))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.pref_log),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.pref_log_desc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.pref_log_level),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LogLevelDropdown(logLevel) { level ->
                        logLevel = level
                        AppPrefs.logLevel = level
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        stringResource(R.string.pref_log_format),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LogFormatDropdown(logFormat) { format ->
                        logFormat = format
                        AppPrefs.logFormat = format
                    }
                }
            }

            // ── About ──
            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(stringResource(R.string.section_about))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInstructionsDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.pref_instructions),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.pref_instructions_desc),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                val uriHandler = LocalUriHandler.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://github.com/Akiyama-25/NCM-Converter-For-Android") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.pref_github),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.pref_github_url),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // ── FlClash-Style System Installed Font Picker ModalBottomSheet ──
    if (showFontPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val filteredFonts = remember(systemFonts, fontSearchQuery) {
            if (fontSearchQuery.isBlank()) systemFonts
            else systemFonts.filter {
                it.displayName.contains(fontSearchQuery, ignoreCase = true) ||
                    it.subtitle.contains(fontSearchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showFontPickerSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.82f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.font_picker_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 17.sp
                        )
                        Text(
                            text = stringResource(R.string.font_scanned_count, systemFonts.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            AppPrefs.systemFontId = SystemFontScanner.ID_SYSTEM_DEFAULT
                            fontWeight = 400
                            AppPrefs.fontWeight = 400
                        }
                    ) {
                        Text(stringResource(R.string.font_reset_default))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = fontSearchQuery,
                    onValueChange = { fontSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("system_font_search_input"),
                    placeholder = { Text(stringResource(R.string.font_search_hint), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (fontSearchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { fontSearchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = null)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(filteredFonts, key = { it.id }) { entry ->
                        val isSelected = entry.id == systemFontId
                        val itemFontFamily = remember(entry.id) {
                            SystemFontScanner.resolveFontFamily(entry.id)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    AppPrefs.systemFontId = entry.id
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            border = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = entry.displayName,
                                            fontFamily = itemFontFamily,
                                            fontWeight = FontWeight(fontWeight.coerceIn(100, 900)),
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (entry.isBuiltInLogical) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "OS",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.font_preview_sample),
                                        fontFamily = itemFontFamily,
                                        fontWeight = FontWeight(fontWeight.coerceIn(100, 900)),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = entry.subtitle,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showColumnsDialog) {
        AlertDialog(
            onDismissRequest = {
                showColumnsDialog = false
                gridColumns = AppPrefs.gridColumns
            },
            title = { Text(stringResource(R.string.pref_grid_columns)) },
            text = { Text(stringResource(R.string.columns_dialog_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showColumnsDialog = false
                    AppPrefs.gridColumns = 1
                }) { Text(stringResource(R.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showColumnsDialog = false
                    gridColumns = AppPrefs.gridColumns
                }) { Text(stringResource(R.string.home_cancel)) }
            }
        )
    }

    if (showLanguageChangeDialog) {
        val targetContext = remember(pendingLanguageCode) {
            val locale = if (pendingLanguageCode == "system") java.util.Locale.getDefault()
            else java.util.Locale.forLanguageTag(pendingLanguageCode)
            val config = Configuration(context.resources.configuration).apply {
                this.locale = locale
            }
            context.createConfigurationContext(config)
        }
        AlertDialog(
            onDismissRequest = { showLanguageChangeDialog = false },
            title = { Text(targetContext.getString(R.string.lang_change_title)) },
            text = { Text(targetContext.getString(R.string.lang_change_message)) },
            confirmButton = {
                TextButton(onClick = { showLanguageChangeDialog = false }) {
                    Text(targetContext.getString(R.string.lang_change_confirm))
                }
            }
        )
    }

    if (showInstructionsDialog) {
        val instructionsText = remember(appLanguage) {
            try {
                val resContext = if (appLanguage == "system") {
                    context
                } else {
                    val locale = java.util.Locale.forLanguageTag(appLanguage)
                    val config = Configuration(context.resources.configuration).apply {
                        setLocale(locale)
                    }
                    context.createConfigurationContext(config)
                }
                resContext.resources.openRawResource(R.raw.app_instructions)
                    .bufferedReader().use { it.readText() }
            } catch (_: Exception) {
                context.getString(R.string.instructions_load_failed)
            }
        }
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = { Text(stringResource(R.string.instructions_title)) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownRenderer(markdown = instructionsText)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) {
                    Text(stringResource(R.string.instructions_close))
                }
            }
        )
    }

    if (showHslSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val previewColor = hslToColor(sliderH, sliderS, sliderL)

        LaunchedEffect(sliderH, sliderS, sliderL) {
            val argb = hslToArgbLong(sliderH, sliderS, sliderL)
            when (hslTarget) {
                HslTarget.ACCENT -> {
                    accentColor = argb
                    AppPrefs.updateAccentColor(argb, sliderH, sliderS, sliderL)
                }
                HslTarget.BG -> {
                    if (isDark) darkBgColor = argb else lightBgColor = argb
                    AppPrefs.updateBgColor(argb, sliderH, sliderS, sliderL, isDark)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showHslSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(
                        if (hslTarget == HslTarget.ACCENT) R.string.pref_custom_hsl_accent else R.string.pref_custom_hsl_bg
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HslSlider(
                    label = stringResource(R.string.pref_hsl_hue),
                    value = sliderH,
                    valueRange = 0f..360f,
                    gradientColors = hueGradientColors(),
                    valueText = "${sliderH.toInt()}°",
                    onValueChange = { sliderH = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HslSlider(
                    label = stringResource(R.string.pref_hsl_lightness),
                    value = sliderL,
                    valueRange = 0f..100f,
                    gradientColors = listOf(
                        Color.Black,
                        hslToColor(sliderH, sliderS, 50f),
                        Color.White
                    ),
                    valueText = "${sliderL.toInt()}%",
                    onValueChange = { sliderL = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HslSlider(
                    label = stringResource(R.string.pref_hsl_saturation),
                    value = sliderS,
                    valueRange = 0f..100f,
                    gradientColors = listOf(
                        Color(0xFF808080),
                        hslToColor(sliderH, 100f, sliderL)
                    ),
                    valueText = "${sliderS.toInt()}%",
                    onValueChange = { sliderS = it }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showLogDialog) {
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.log_dialog_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "($logLevel · .$logFormat)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 450.dp)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        ) {
                            if (logFormat.equals("md", ignoreCase = true)) {
                                MarkdownRenderer(markdown = logText)
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = logText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AdaptiveControlsRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalSpacing = 8.dp,
                        verticalSpacing = 8.dp
                    ) {
                        OutlinedButton(
                            onClick = {
                                AppLogger.clearLogs()
                                logText = AppLogger.getLogContent(context, logLevel, logFormat)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.log_clear_confirm),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.heightIn(min = 42.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.log_action_clear),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                AppLogger.copyToClipboard(context, logLevel, logFormat)
                            },
                            modifier = Modifier.heightIn(min = 42.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.log_action_copy),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                AppLogger.exportAndShare(context, logLevel, logFormat)
                            },
                            modifier = Modifier.heightIn(min = 42.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.log_action_export),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLogDialog = false }) {
                    Text(stringResource(R.string.instructions_close))
                }
            }
        )
    }
}

@Composable
private fun SystemFontSettingCard(
    selectedFontId: String,
    fontWeight: Int,
    scannedCount: Int,
    onOpenPicker: () -> Unit,
    onWeightChanged: (Int) -> Unit,
    onResetDefault: () -> Unit
) {
    val defaultFontLabel = stringResource(R.string.font_system)
    val activeFontName = remember(selectedFontId, scannedCount, defaultFontLabel) {
        if (selectedFontId == SystemFontScanner.ID_SYSTEM_DEFAULT || selectedFontId.isBlank()) {
            defaultFontLabel
        } else {
            SystemFontScanner.getDisplayName(selectedFontId)
        }
    }
    val rawWeightIndex = AppFontWeights.indexOfFirst { it.first.weight == fontWeight }
    val weightIndex = if (rawWeightIndex >= 0) rawWeightIndex else 3 // 400 Regular
    val weightLabel = AppFontWeights.getOrNull(weightIndex)?.second ?: "Regular ($fontWeight)"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("system_font_setting_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenPicker() }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.FontDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.pref_font),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.font_scanned_count, scannedCount),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.widthIn(max = 160.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeFontName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.UnfoldMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Typography Preview Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPicker() },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = stringResource(R.string.font_preview_sample),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Font Weight Step Slider (100..900)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.font_weight),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = weightLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedFontId != SystemFontScanner.ID_SYSTEM_DEFAULT || fontWeight != 400) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.font_reset_default),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.clickable { onResetDefault() }
                        )
                    }
                }
            }

            Slider(
                value = weightIndex.toFloat(),
                onValueChange = { idx ->
                    val newWeight = AppFontWeights[idx.toInt().coerceIn(0, AppFontWeights.lastIndex)].first.weight
                    onWeightChanged(newWeight)
                },
                valueRange = 0f..(AppFontWeights.size - 1).toFloat(),
                steps = AppFontWeights.size - 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("font_weight_slider")
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ColorPickerRow(
    colors: List<ColorOption>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onCustomClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        colors.forEach { option ->
            val isSelected = selectedColor.toArgb() == option.color.toArgb()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(option.color)
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        }
                    )
                    .clickable { onColorSelected(option.color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = if (option.color.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color.Red, Color.Yellow, Color.Cyan,
                            Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable { onCustomClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.pref_custom_hsl_accent),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.heightIn(min = 38.dp),
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageDropdown(currentCode: String, isDark: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "system" to R.string.lang_system,
        "zh-SC" to R.string.lang_zh,
        "zh-TC" to R.string.lang_zh_tw,
        "en" to R.string.lang_en,
        "ja" to R.string.lang_ja,
    )
    val currentLabel = options.firstOrNull { it.first == currentCode }
        ?.second?.let { stringResource(it) } ?: currentCode

    Box(modifier = Modifier.padding(top = 6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (code, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        onSelect(code)
                        expanded = false
                    },
                    trailingIcon = if (code == currentCode) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HslSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    gradientColors: List<Color>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(valueText, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .drawBehind {
                    val trackHeight = 12.dp.toPx()
                    val trackRadius = trackHeight / 2
                    val yCenter = size.height / 2
                    drawRoundRect(
                        brush = Brush.horizontalGradient(gradientColors),
                        topLeft = Offset(0f, yCenter - trackHeight / 2),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(trackRadius, trackRadius)
                    )
                },
            thumb = {
                val interactionSource = remember { MutableInteractionSource() }
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(24.dp, 24.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(32.dp),
                    colors = SliderDefaults.colors(
                        inactiveTrackColor = Color.Transparent,
                        activeTrackColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        activeTickColor = Color.Transparent
                    )
                )
            }
        )
    }
}

private fun hueGradientColors(): List<Color> {
    val steps = 7
    return (0..steps).map { i ->
        hslToColor(h = i * 360f / steps, s = 100f, l = 50f)
    }
}

@Composable
private fun LogLevelDropdown(currentLevel: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "VERBOSE",
        "DEBUG",
        "INFO",
        "WARN",
        "ERROR"
    )

    Box(modifier = Modifier.padding(top = 6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLevel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { level ->
                DropdownMenuItem(
                    text = { Text(level) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    },
                    trailingIcon = if (level == currentLevel) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun LogFormatDropdown(currentFormat: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val defaultLogLabel = stringResource(R.string.log_format_default)
    val options = listOf(
        "log" to defaultLogLabel,
        "txt" to ".txt",
        "md" to ".md"
    )
    val currentLabel = options.firstOrNull { it.first.equals(currentFormat, ignoreCase = true) }?.second ?: ".$currentFormat"

    Box(modifier = Modifier.padding(top = 6.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentLabel,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (format, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(format)
                        expanded = false
                    },
                    trailingIcon = if (format.equals(currentFormat, ignoreCase = true)) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
            }
        }
    }
}
