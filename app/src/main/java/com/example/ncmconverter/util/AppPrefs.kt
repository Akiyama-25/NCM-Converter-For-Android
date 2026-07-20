package com.example.ncmconverter.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppPrefs {
    private const val NAME = "ncm_converter_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_CUSTOM_PATH = "custom_path"
    private const val KEY_AUTO_SAVE = "auto_save"
    private const val KEY_ENABLE_LYRIC = "enable_lyric"
    private const val KEY_LYRIC_MODE = "lyric_mode"
    private const val KEY_LYRIC_API_BASE_URL = "lyric_api_base_url"
    private const val KEY_LYRIC_REAL_IP = "lyric_real_ip"
    private const val KEY_ENABLE_COVER = "enable_cover"
    private const val KEY_USE_MONET_COLORS = "use_monet_colors"
    private const val KEY_CUSTOM_ACCENT_COLOR = "custom_accent_color"
    private const val KEY_CUSTOM_LIGHT_BG_COLOR = "custom_light_bg_color"
    private const val KEY_CUSTOM_DARK_BG_COLOR = "custom_dark_bg_color"
    private const val KEY_CUSTOM_ACCENT_H = "custom_accent_h"
    private const val KEY_CUSTOM_ACCENT_S = "custom_accent_s"
    private const val KEY_CUSTOM_ACCENT_L = "custom_accent_l"
    private const val KEY_CUSTOM_BG_H = "custom_bg_h"
    private const val KEY_CUSTOM_BG_S = "custom_bg_s"
    private const val KEY_CUSTOM_BG_L = "custom_bg_l"
    private const val KEY_GRID_COLUMNS = "grid_columns"
    private const val KEY_PORTRAIT_GRID_ENABLED = "portrait_grid_enabled"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_USE_EMBEDDED_FONT = "use_embedded_font"
    private const val KEY_FONT_WEIGHT = "font_weight"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    const val LYRIC_MODE_MERGED = "merged"
    const val LYRIC_MODE_RAW = "raw"
    const val LYRIC_MODE_TRANSLATED = "translated"

    private lateinit var prefs: SharedPreferences

    private val _themeFlow = MutableStateFlow(THEME_SYSTEM)
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    const val DEFAULT_ACCENT_COLOR: Long = 0xFF1DB954
    const val DEFAULT_LIGHT_BG_COLOR: Long = 0xFFFAFAFA
    const val DEFAULT_DARK_BG_COLOR: Long = 0xFF121212

    private val _monetFlow = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    val monetFlow: StateFlow<Boolean> = _monetFlow.asStateFlow()

    private val _accentColorFlow = MutableStateFlow(DEFAULT_ACCENT_COLOR)
    val accentColorFlow: StateFlow<Long> = _accentColorFlow.asStateFlow()

    private val _lightBgColorFlow = MutableStateFlow(DEFAULT_LIGHT_BG_COLOR)
    val lightBgColorFlow: StateFlow<Long> = _lightBgColorFlow.asStateFlow()

    private val _darkBgColorFlow = MutableStateFlow(DEFAULT_DARK_BG_COLOR)
    val darkBgColorFlow: StateFlow<Long> = _darkBgColorFlow.asStateFlow()

    private val _accentHFlow = MutableStateFlow(141f)
    val accentHFlow: StateFlow<Float> = _accentHFlow.asStateFlow()

    private val _accentSFlow = MutableStateFlow(73f)
    val accentSFlow: StateFlow<Float> = _accentSFlow.asStateFlow()

    private val _accentLFlow = MutableStateFlow(41f)
    val accentLFlow: StateFlow<Float> = _accentLFlow.asStateFlow()

    private val _bgHFlow = MutableStateFlow(0f)
    val bgHFlow: StateFlow<Float> = _bgHFlow.asStateFlow()

    private val _bgSFlow = MutableStateFlow(0f)
    val bgSFlow: StateFlow<Float> = _bgSFlow.asStateFlow()

    private val _bgLFlow = MutableStateFlow(98f)
    val bgLFlow: StateFlow<Float> = _bgLFlow.asStateFlow()

    private val _gridColumnsFlow = MutableStateFlow(2)
    val gridColumnsFlow: StateFlow<Int> = _gridColumnsFlow.asStateFlow()

    private val _portraitGridEnabledFlow = MutableStateFlow(false)
    val portraitGridEnabledFlow: StateFlow<Boolean> = _portraitGridEnabledFlow.asStateFlow()

    private val _useEmbeddedFontFlow = MutableStateFlow(false)
    val useEmbeddedFontFlow: StateFlow<Boolean> = _useEmbeddedFontFlow.asStateFlow()

    private val _fontWeightFlow = MutableStateFlow(400)
    val fontWeightFlow: StateFlow<Int> = _fontWeightFlow.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        _themeFlow.value = themeMode // sync initial state
        _monetFlow.value = useMonetColors
        _accentColorFlow.value = customAccentColor
        _lightBgColorFlow.value = customLightBgColor
        _darkBgColorFlow.value = customDarkBgColor
        _accentHFlow.value = customAccentH
        _accentSFlow.value = customAccentS
        _accentLFlow.value = customAccentL
        _bgHFlow.value = customBgH
        _bgSFlow.value = customBgS
        _bgLFlow.value = customBgL
        _gridColumnsFlow.value = gridColumns
        _portraitGridEnabledFlow.value = portraitGridEnabled
        _useEmbeddedFontFlow.value = useEmbeddedFont
        _fontWeightFlow.value = fontWeight
    }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value).apply()
            _themeFlow.value = value
        }

    var customPath: String
        get() = prefs.getString(KEY_CUSTOM_PATH, "Music/NCMConverter") ?: "Music/NCMConverter"
        set(value) = prefs.edit().putString(KEY_CUSTOM_PATH, value).apply()

    var autoSave: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SAVE, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()

    var enableLyric: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_LYRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_LYRIC, value).apply()

    var lyricMode: String
        get() = prefs.getString(KEY_LYRIC_MODE, LYRIC_MODE_MERGED) ?: LYRIC_MODE_MERGED
        set(value) = prefs.edit().putString(KEY_LYRIC_MODE, value).apply()

    var lyricApiBaseUrl: String
        get() = prefs.getString(KEY_LYRIC_API_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LYRIC_API_BASE_URL, value).apply()

    var lyricRealIP: String
        get() = prefs.getString(KEY_LYRIC_REAL_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LYRIC_REAL_IP, value).apply()

    var enableCover: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_COVER, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_COVER, value).apply()

    var useMonetColors: Boolean
        get() = prefs.getBoolean(KEY_USE_MONET_COLORS, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_MONET_COLORS, value).apply()
            _monetFlow.value = value
        }

    var customAccentColor: Long
        get() = prefs.getLong(KEY_CUSTOM_ACCENT_COLOR, DEFAULT_ACCENT_COLOR)
        set(value) {
            prefs.edit().putLong(KEY_CUSTOM_ACCENT_COLOR, value).apply()
            _accentColorFlow.value = value
        }

    var customLightBgColor: Long
        get() = prefs.getLong(KEY_CUSTOM_LIGHT_BG_COLOR, DEFAULT_LIGHT_BG_COLOR)
        set(value) {
            prefs.edit().putLong(KEY_CUSTOM_LIGHT_BG_COLOR, value).apply()
            _lightBgColorFlow.value = value
        }

    var customDarkBgColor: Long
        get() = prefs.getLong(KEY_CUSTOM_DARK_BG_COLOR, DEFAULT_DARK_BG_COLOR)
        set(value) {
            prefs.edit().putLong(KEY_CUSTOM_DARK_BG_COLOR, value).apply()
            _darkBgColorFlow.value = value
        }

    var customAccentH: Float
        get() = prefs.getFloat(KEY_CUSTOM_ACCENT_H, 141f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_ACCENT_H, value).apply()
            _accentHFlow.value = value
        }

    var customAccentS: Float
        get() = prefs.getFloat(KEY_CUSTOM_ACCENT_S, 73f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_ACCENT_S, value).apply()
            _accentSFlow.value = value
        }

    var customAccentL: Float
        get() = prefs.getFloat(KEY_CUSTOM_ACCENT_L, 41f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_ACCENT_L, value).apply()
            _accentLFlow.value = value
        }

    var customBgH: Float
        get() = prefs.getFloat(KEY_CUSTOM_BG_H, 0f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_BG_H, value).apply()
            _bgHFlow.value = value
        }

    var customBgS: Float
        get() = prefs.getFloat(KEY_CUSTOM_BG_S, 0f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_BG_S, value).apply()
            _bgSFlow.value = value
        }

    var customBgL: Float
        get() = prefs.getFloat(KEY_CUSTOM_BG_L, 98f)
        set(value) {
            prefs.edit().putFloat(KEY_CUSTOM_BG_L, value).apply()
            _bgLFlow.value = value
        }

    var gridColumns: Int
        get() = prefs.getInt(KEY_GRID_COLUMNS, 2)
        set(value) {
            prefs.edit().putInt(KEY_GRID_COLUMNS, value.coerceIn(1, 5)).apply()
            _gridColumnsFlow.value = value.coerceIn(1, 5)
        }

    var portraitGridEnabled: Boolean
        get() = prefs.getBoolean(KEY_PORTRAIT_GRID_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_PORTRAIT_GRID_ENABLED, value).apply()
            _portraitGridEnabledFlow.value = value
        }

    var useEmbeddedFont: Boolean
        get() = prefs.getBoolean(KEY_USE_EMBEDDED_FONT, true)
        set(value) {
            prefs.edit().putBoolean(KEY_USE_EMBEDDED_FONT, value).apply()
            _useEmbeddedFontFlow.value = value
        }

    var fontWeight: Int
        get() = prefs.getInt(KEY_FONT_WEIGHT, 400)
        set(value) {
            prefs.edit().putInt(KEY_FONT_WEIGHT, value.coerceIn(200, 900)).apply()
            _fontWeightFlow.value = value.coerceIn(200, 900)
        }

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    fun updateAccentColor(argb: Long, h: Float, s: Float, l: Float) {
        prefs.edit()
            .putLong(KEY_CUSTOM_ACCENT_COLOR, argb)
            .putFloat(KEY_CUSTOM_ACCENT_H, h)
            .putFloat(KEY_CUSTOM_ACCENT_S, s)
            .putFloat(KEY_CUSTOM_ACCENT_L, l)
            .apply()
        _accentColorFlow.value = argb
        _accentHFlow.value = h
        _accentSFlow.value = s
        _accentLFlow.value = l
    }

    fun updateBgColor(argb: Long, h: Float, s: Float, l: Float, isDark: Boolean) {
        val editor = prefs.edit()
            .putFloat(KEY_CUSTOM_BG_H, h)
            .putFloat(KEY_CUSTOM_BG_S, s)
            .putFloat(KEY_CUSTOM_BG_L, l)
        if (isDark) {
            editor.putLong(KEY_CUSTOM_DARK_BG_COLOR, argb)
        } else {
            editor.putLong(KEY_CUSTOM_LIGHT_BG_COLOR, argb)
        }
        editor.apply()
        _bgHFlow.value = h
        _bgSFlow.value = s
        _bgLFlow.value = l
        if (isDark) _darkBgColorFlow.value = argb else _lightBgColorFlow.value = argb
    }
}
