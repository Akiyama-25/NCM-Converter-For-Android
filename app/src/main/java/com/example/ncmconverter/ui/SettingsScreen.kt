package com.example.ncmconverter.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ncmconverter.ui.theme.colorToHsl
import com.example.ncmconverter.ui.theme.hslToArgbLong
import com.example.ncmconverter.ui.theme.hslToColor
import com.example.ncmconverter.util.AppPrefs

private data class ColorOption(val label: String, val color: Color)

private val AccentColors = listOf(
    ColorOption("绿色", Color(0xFF1DB954)),
    ColorOption("蓝色", Color(0xFF1976D2)),
    ColorOption("紫色", Color(0xFF7B1FA2)),
    ColorOption("红色", Color(0xFFD32F2F)),
    ColorOption("橙色", Color(0xFFF57C00)),
    ColorOption("青色", Color(0xFF00838F)),
    ColorOption("粉色", Color(0xFFC2185B)),
    ColorOption("靛蓝", Color(0xFF303F9F)),
)

private val LightBgColors = listOf(
    ColorOption("白色", Color(0xFFFAFAFA)),
    ColorOption("浅灰", Color(0xFFF5F5F5)),
    ColorOption("米白", Color(0xFFFFF8E1)),
    ColorOption("浅蓝", Color(0xFFE3F2FD)),
)

private val DarkBgColors = listOf(
    ColorOption("深黑", Color(0xFF121212)),
    ColorOption("深灰", Color(0xFF1E1E1E)),
    ColorOption("深蓝", Color(0xFF0D1B2A)),
    ColorOption("深紫", Color(0xFF1A1A2E)),
)

private enum class HslTarget { ACCENT, BG }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var themeMode by remember { mutableStateOf(AppPrefs.themeMode) }
    var customPath by remember { mutableStateOf(AppPrefs.customPath) }
    var manualSave by remember { mutableStateOf(AppPrefs.manualSave) }
    var enableLyric by remember { mutableStateOf(AppPrefs.enableLyric) }
    var lyricMode by remember { mutableStateOf(AppPrefs.lyricMode) }
    var lyricApiBaseUrl by remember { mutableStateOf(AppPrefs.lyricApiBaseUrl) }
    var lyricRealIP by remember { mutableStateOf(AppPrefs.lyricRealIP) }
    var enableCover by remember { mutableStateOf(AppPrefs.enableCover) }
    var useMonet by remember { mutableStateOf(AppPrefs.useMonetColors) }
    var accentColor by remember { mutableLongStateOf(AppPrefs.customAccentColor) }
    var lightBgColor by remember { mutableLongStateOf(AppPrefs.customLightBgColor) }
    var darkBgColor by remember { mutableLongStateOf(AppPrefs.customDarkBgColor) }

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

    // HSL BottomSheet state
    var showHslSheet by remember { mutableStateOf(false) }
    var hslTarget by remember { mutableStateOf(HslTarget.ACCENT) }
    var sliderH by remember { mutableFloatStateOf(0f) }
    var sliderS by remember { mutableFloatStateOf(0f) }
    var sliderL by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // ===== 分类 1：显示 =====
            SectionHeader("显示")

            // 主题模式
            Text("主题模式", modifier = Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeChip("跟随系统", themeMode == AppPrefs.THEME_SYSTEM) {
                    themeMode = AppPrefs.THEME_SYSTEM; AppPrefs.themeMode = themeMode
                }
                ThemeChip("浅色", themeMode == AppPrefs.THEME_LIGHT) {
                    themeMode = AppPrefs.THEME_LIGHT; AppPrefs.themeMode = themeMode
                }
                ThemeChip("深色", themeMode == AppPrefs.THEME_DARK) {
                    themeMode = AppPrefs.THEME_DARK; AppPrefs.themeMode = themeMode
                }
            }

            // 动态取色开关 (仅 Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val currentColorScheme = MaterialTheme.colorScheme
                SettingSwitch(
                    title = "动态取色 (Material You)",
                    subtitle = if (useMonet) "从壁纸提取主题色" else "使用自定义颜色",
                    checked = useMonet,
                    onCheckedChange = { newValue ->
                        if (!newValue) {
                            // 关闭 Monet 时，捕获当前壁纸色保持不变
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

            // 手动颜色选择器 (仅在动态取色关闭时)
            if (!useMonet) {
                // 强调色
                Text("强调色", modifier = Modifier.padding(start = 16.dp, top = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                // 背景色
                Text("背景色", modifier = Modifier.padding(start = 16.dp, top = 12.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== 分类 2：转换 =====
            SectionHeader("转换")

            SectionHeader("导出")
            OutlinedTextField(
                value = customPath,
                onValueChange = { customPath = it; AppPrefs.customPath = it },
                label = { Text("保存路径") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
            SettingSwitch(
                title = "手动点击保存",
                subtitle = if (manualSave) "转换完成后需手动点击保存按钮" else "转换完成后自动保存",
                checked = manualSave,
                onCheckedChange = { manualSave = it; AppPrefs.manualSave = it }
            )
            SettingSwitch(
                title = "自动获取封面",
                subtitle = if (enableCover) "转换时自动从网络下载歌曲封面" else "不下载封面图片（离线可用）",
                checked = enableCover,
                onCheckedChange = { enableCover = it; AppPrefs.enableCover = it }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SectionHeader("歌词")
            SettingSwitch(
                title = "自动嵌入歌词",
                subtitle = if (enableLyric) "转换完成后自动搜索并嵌入歌词" else "不自动获取歌词",
                checked = enableLyric,
                onCheckedChange = { enableLyric = it; AppPrefs.enableLyric = it }
            )

            if (enableLyric) {
                Text("歌词模式", modifier = Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LyricModeChip("混合模式", lyricMode == AppPrefs.LYRIC_MODE_MERGED) {
                        lyricMode = AppPrefs.LYRIC_MODE_MERGED; AppPrefs.lyricMode = lyricMode
                    }
                    LyricModeChip("原文", lyricMode == AppPrefs.LYRIC_MODE_RAW) {
                        lyricMode = AppPrefs.LYRIC_MODE_RAW; AppPrefs.lyricMode = lyricMode
                    }
                    LyricModeChip("翻译", lyricMode == AppPrefs.LYRIC_MODE_TRANSLATED) {
                        lyricMode = AppPrefs.LYRIC_MODE_TRANSLATED; AppPrefs.lyricMode = lyricMode
                    }
                }
                Text(
                    when (lyricMode) {
                        AppPrefs.LYRIC_MODE_MERGED -> "原文与翻译逐行合并显示"
                        AppPrefs.LYRIC_MODE_RAW -> "仅显示原文歌词"
                        AppPrefs.LYRIC_MODE_TRANSLATED -> "仅显示翻译歌词"
                        else -> ""
                    },
                    modifier = Modifier.padding(start = 16.dp),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = lyricApiBaseUrl,
                    onValueChange = { lyricApiBaseUrl = it; AppPrefs.lyricApiBaseUrl = it },
                    label = { Text("API 地址 *") },
                    placeholder = { Text("https://your-api-domain.com") },
                    supportingText = {
                        Text(
                            "必填，需自行搭建 NeteaseCloudMusicApiEnhanced 服务\n参考: github.com/NeteaseCloudMusicApiEnhanced/api-enhanced",
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                OutlinedTextField(
                    value = lyricRealIP,
                    onValueChange = { lyricRealIP = it; AppPrefs.lyricRealIP = it },
                    label = { Text("Real IP") },
                    placeholder = { Text("可选，如 36.149.92.99") },
                    supportingText = {
                        Text(
                            "留空则不传递 realIP 参数（部分 API 部署需要此参数）",
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // ===== 分类 3：关于 APP =====
            SectionHeader("关于 APP")

            // 使用说明
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
                    Text("使用说明", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("查看应用使用帮助", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // GitHub 链接
            val uriHandler = LocalUriHandler.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri("https://github.com/Akiyama-25/NCM-Converter-For-Android") }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "NCM Converter for Android",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "github.com/Akiyama-25/NCM-Converter-For-Android",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 使用说明弹窗
    if (showInstructionsDialog) {
        val instructionsText = remember {
            try {
                context.resources.openRawResource(com.example.ncmconverter.R.raw.app_instructions)
                    .bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "说明文件加载失败"
            }
        }
        AlertDialog(
            onDismissRequest = { showInstructionsDialog = false },
            title = { Text("使用说明") },
            text = {
                Text(
                    instructionsText,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInstructionsDialog = false }) { Text("关闭") }
            }
        )
    }

    // HSL 自定义颜色 BottomSheet
    if (showHslSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val previewColor = hslToColor(sliderH, sliderS, sliderL)

        // 滑条拖动时实时同步到 AppPrefs
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
                    text = if (hslTarget == HslTarget.ACCENT) "自定义强调色" else "自定义背景色",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 颜色预览圆块
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Spacer(modifier = Modifier.height(24.dp))

                HslSlider(
                    label = "色相",
                    value = sliderH,
                    valueRange = 0f..360f,
                    gradientColors = hueGradientColors(),
                    valueText = "${sliderH.toInt()}°",
                    onValueChange = { sliderH = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HslSlider(
                    label = "饱和度",
                    value = sliderS,
                    valueRange = 0f..100f,
                    gradientColors = listOf(
                        Color(0xFF808080),
                        hslToColor(sliderH, 100f, sliderL)
                    ),
                    valueText = "${sliderS.toInt()}%",
                    onValueChange = { sliderS = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                HslSlider(
                    label = "明度",
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
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
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
        // 自定义颜色按钮（彩虹渐变 + 编辑图标）
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
                contentDescription = "自定义",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RowScope.ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.weight(1f)
    )
}

@Composable
private fun RowScope.LyricModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        modifier = Modifier.weight(1f)
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
            Text(title, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
