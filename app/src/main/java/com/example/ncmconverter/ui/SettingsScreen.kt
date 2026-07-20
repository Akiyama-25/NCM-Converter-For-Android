package com.example.ncmconverter.ui

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ncmconverter.R
import com.example.ncmconverter.ui.theme.AppFontWeights
import com.example.ncmconverter.ui.theme.RhrFontFamily
import com.example.ncmconverter.ui.theme.colorToHsl
import com.example.ncmconverter.ui.theme.hslToArgbLong
import com.example.ncmconverter.ui.theme.hslToColor
import com.example.ncmconverter.util.AppPrefs

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
fun SettingsScreen(onBack: () -> Unit) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as Activity

    var themeMode by remember { mutableStateOf(AppPrefs.themeMode) }
    var customPath by remember { mutableStateOf(AppPrefs.customPath) }
    var autoSave by remember { mutableStateOf(AppPrefs.autoSave) }
    var enableLyric by remember { mutableStateOf(AppPrefs.enableLyric) }
    var lyricMode by remember { mutableStateOf(AppPrefs.lyricMode) }
    var lyricApiBaseUrl by remember { mutableStateOf(AppPrefs.lyricApiBaseUrl) }
    var lyricRealIP by remember { mutableStateOf(AppPrefs.lyricRealIP) }
    var enableCover by remember { mutableStateOf(AppPrefs.enableCover) }
    var useMonet by remember { mutableStateOf(AppPrefs.useMonetColors) }
    var gridColumns by remember { mutableIntStateOf(AppPrefs.gridColumns) }
    val appliedGridColumns by AppPrefs.gridColumnsFlow.collectAsState()
    var portraitGridEnabled by remember { mutableStateOf(AppPrefs.portraitGridEnabled) }
    var accentColor by remember { mutableLongStateOf(AppPrefs.customAccentColor) }
    var lightBgColor by remember { mutableLongStateOf(AppPrefs.customLightBgColor) }
    var darkBgColor by remember { mutableLongStateOf(AppPrefs.customDarkBgColor) }
    var appLanguage by remember { mutableStateOf(AppPrefs.appLanguage) }
    var useEmbeddedFont by remember { mutableStateOf(AppPrefs.useEmbeddedFont) }
    var fontWeight by remember { mutableIntStateOf(AppPrefs.fontWeight) }

    val isDark = when (themeMode) {
        AppPrefs.THEME_DARK -> true
        AppPrefs.THEME_LIGHT -> false
        else -> {
            val nightMode = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Display ──
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.section_display)) }

            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.pref_theme_mode), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Text(stringResource(R.string.pref_language), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text(stringResource(R.string.pref_grid_columns), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.pref_grid_columns_value, gridColumns), fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                        subtitle = stringResource(if (portraitGridEnabled) R.string.pref_portrait_grid_on else R.string.pref_portrait_grid_off),
                        checked = portraitGridEnabled,
                        onCheckedChange = { portraitGridEnabled = it; AppPrefs.portraitGridEnabled = it }
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                val uriHandler = LocalUriHandler.current
                FontSettingRow(
                    useEmbeddedFont = useEmbeddedFont,
                    onToggle = { useEmbeddedFont = it; AppPrefs.useEmbeddedFont = it },
                    onLinkClick = { uriHandler.openUri("https://github.com/CyanoHao/Resource-Han-Rounded") }
                )
            }

            item(key = "font_weight", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = useEmbeddedFont,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    val weightIndex = AppFontWeights.indexOfFirst { it.first.weight == fontWeight }
                        .coerceAtLeast(2)
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Slider(
                            value = weightIndex.toFloat(),
                            onValueChange = { idx ->
                                val newWeight = AppFontWeights[idx.toInt()].first.weight
                                fontWeight = newWeight
                                AppPrefs.fontWeight = newWeight
                            },
                            valueRange = 0f..(AppFontWeights.size - 1).toFloat(),
                            steps = AppFontWeights.size - 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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
                        Text(stringResource(R.string.pref_accent_color), modifier = Modifier.padding(start = 16.dp, top = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(stringResource(R.string.pref_bg_color), modifier = Modifier.padding(start = 16.dp, top = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item(span = { GridItemSpan(maxLineSpan) }) { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.section_convert)) }

            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it; AppPrefs.customPath = it },
                    label = { Text(stringResource(R.string.pref_save_path)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
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
            item(span = { GridItemSpan(maxLineSpan) }) { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.section_lyrics)) }

            item {
                SettingSwitch(
                    title = stringResource(R.string.pref_auto_lyric),
                    subtitle = stringResource(if (enableLyric) R.string.pref_auto_lyric_on else R.string.pref_auto_lyric_off),
                    checked = enableLyric,
                    onCheckedChange = { enableLyric = it; AppPrefs.enableLyric = it }
                )
            }

            item(key = "lyric_mode") {
                AnimatedVisibility(
                    visible = enableLyric,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.pref_lyric_mode), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            LyricModeChip(stringResource(R.string.pref_lyric_mode_merged), lyricMode == AppPrefs.LYRIC_MODE_MERGED) {
                                lyricMode = AppPrefs.LYRIC_MODE_MERGED; AppPrefs.lyricMode = lyricMode
                            }
                            LyricModeChip(stringResource(R.string.pref_lyric_mode_raw), lyricMode == AppPrefs.LYRIC_MODE_RAW) {
                                lyricMode = AppPrefs.LYRIC_MODE_RAW; AppPrefs.lyricMode = lyricMode
                            }
                            LyricModeChip(stringResource(R.string.pref_lyric_mode_translated), lyricMode == AppPrefs.LYRIC_MODE_TRANSLATED) {
                                lyricMode = AppPrefs.LYRIC_MODE_TRANSLATED; AppPrefs.lyricMode = lyricMode
                            }
                        }
                        Text(
                            stringResource(
                                when (lyricMode) {
                                    AppPrefs.LYRIC_MODE_MERGED -> R.string.pref_lyric_mode_merged_desc
                                    AppPrefs.LYRIC_MODE_RAW -> R.string.pref_lyric_mode_raw_desc
                                    AppPrefs.LYRIC_MODE_TRANSLATED -> R.string.pref_lyric_mode_translated_desc
                                    else -> R.string.pref_lyric_mode_merged_desc
                                }
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "lyric_api", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = enableLyric,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    OutlinedTextField(
                        value = lyricApiBaseUrl,
                        onValueChange = { lyricApiBaseUrl = it; AppPrefs.lyricApiBaseUrl = it },
                        label = { Text(stringResource(R.string.pref_lyric_api)) },
                        placeholder = { Text(stringResource(R.string.pref_lyric_api_hint)) },
                        supportingText = { Text(stringResource(R.string.pref_lyric_api_support), fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

            item(key = "lyric_ip", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = enableLyric,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                ) {
                    OutlinedTextField(
                        value = lyricRealIP,
                        onValueChange = { lyricRealIP = it; AppPrefs.lyricRealIP = it },
                        label = { Text(stringResource(R.string.pref_real_ip)) },
                        placeholder = { Text(stringResource(R.string.pref_real_ip_hint)) },
                        supportingText = { Text(stringResource(R.string.pref_real_ip_support), fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

            // ── About ──
            item(span = { GridItemSpan(maxLineSpan) }) { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeader(stringResource(R.string.section_about)) }

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
                        Text(stringResource(R.string.pref_instructions), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.pref_instructions_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                }) { Text(stringResource(R.string.home_clear_confirm)) }
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
        val instructionsText = remember {
            try {
                context.resources.openRawResource(R.raw.app_instructions)
                    .bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                context.getString(R.string.instructions_load_failed)
            }
        }
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = { Text(stringResource(R.string.instructions_title)) },
            text = {
                Text(
                    instructionsText,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) { Text(stringResource(R.string.instructions_close)) }
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
                    text = stringResource(if (hslTarget == HslTarget.ACCENT) R.string.pref_custom_hsl_accent else R.string.pref_custom_hsl_bg),
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
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
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
private fun RowScope.ThemeChip(label: String, selected: Boolean, isDark: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 14.sp) },
        modifier = Modifier.weight(1f),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = if (isDark) Color(0xFFD0BCFF) else Color(0xFF6750A4),
            selectedLabelColor = if (isDark) Color(0xFF381E72) else Color.White
        )
    )
}

@Composable
private fun LyricModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 14.sp) }
    )
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
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
private fun FontSettingRow(
    useEmbeddedFont: Boolean,
    onToggle: (Boolean) -> Unit,
    onLinkClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!useEmbeddedFont) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.pref_font),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (useEmbeddedFont) RhrFontFamily else FontFamily.Default
            )
            if (useEmbeddedFont) {
                Text(
                    text = stringResource(R.string.font_embedded),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onLinkClick() },
                    fontFamily = RhrFontFamily
                )
            } else {
                Text(
                    stringResource(R.string.font_system),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = useEmbeddedFont, onCheckedChange = onToggle)
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

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(currentLabel, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
