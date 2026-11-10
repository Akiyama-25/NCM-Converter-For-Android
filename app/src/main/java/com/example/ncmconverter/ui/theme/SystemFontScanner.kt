package com.example.ncmconverter.ui.theme

import android.graphics.fonts.SystemFonts
import android.os.Build
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

data class SystemFontEntry(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val isBuiltInLogical: Boolean = false,
    val variantFiles: Map<Int, File> = emptyMap()
)

/**
 * Scans system-installed fonts on the device (inspired by FlClash's system font selector),
 * removing the need for large bundled font assets while allowing live preview and weight control.
 */
object SystemFontScanner {

    const val ID_SYSTEM_DEFAULT = "system"
    const val ID_SANS_SERIF = "sans-serif"
    const val ID_SERIF = "serif"
    const val ID_MONOSPACE = "monospace"
    const val ID_CURSIVE = "cursive"

    private val familyCache = ConcurrentHashMap<String, FontFamily>()
    @Volatile
    private var cachedEntries: List<SystemFontEntry>? = null

    private val excludedKeywords = listOf(
        "emoji", "symbol", "dingbats", "glyphs", "clock", " lock", "weather", "barcode", "braille"
    )

    fun getLogicalPresets(): List<SystemFontEntry> = listOf(
        SystemFontEntry(
            id = ID_SYSTEM_DEFAULT,
            displayName = "System Default",
            subtitle = "OS UI Default Font",
            isBuiltInLogical = true
        ),
        SystemFontEntry(
            id = ID_SANS_SERIF,
            displayName = "Sans-Serif",
            subtitle = "System Sans-Serif",
            isBuiltInLogical = true
        ),
        SystemFontEntry(
            id = ID_SERIF,
            displayName = "Serif",
            subtitle = "System Serif",
            isBuiltInLogical = true
        ),
        SystemFontEntry(
            id = ID_MONOSPACE,
            displayName = "Monospace",
            subtitle = "System Monospace",
            isBuiltInLogical = true
        ),
        SystemFontEntry(
            id = ID_CURSIVE,
            displayName = "Cursive",
            subtitle = "System Script / Cursive",
            isBuiltInLogical = true
        )
    )

    fun scanSystemFonts(forceRefresh: Boolean = false): List<SystemFontEntry> {
        val existing = cachedEntries
        if (!forceRefresh && existing != null) return existing

        val files = LinkedHashSet<File>()

        // 1. Android 10+ SystemFonts API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val sysFonts = SystemFonts.getAvailableFonts()
                for (font in sysFonts) {
                    val f = font.file
                    if (f != null && f.exists() && f.canRead()) {
                        files.add(f)
                    }
                }
            } catch (_: Throwable) {
            }
        }

        // 2. Standard system font directories
        val fontDirs = listOf(
            "/system/fonts",
            "/product/fonts",
            "/system/product/fonts",
            "/vendor/fonts"
        )
        for (dirPath in fontDirs) {
            try {
                val dir = File(dirPath)
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.canRead()) {
                            val lower = file.name.lowercase()
                            if (lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")) {
                                files.add(file)
                            }
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }

        // 3. Group font files into families with weights
        val grouped = LinkedHashMap<String, MutableMap<Int, File>>()
        val displayNames = LinkedHashMap<String, String>()

        for (file in files) {
            val fileNameNoExt = file.nameWithoutExtension
            val lowerName = fileNameNoExt.lowercase()
            if (excludedKeywords.any { lowerName.contains(it) }) continue

            val (familyRaw, weight) = parseFamilyAndWeight(file, fileNameNoExt)
            if (familyRaw.isBlank()) continue

            val normalizedId = "sys:${familyRaw.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
            val variants = grouped.getOrPut(normalizedId) { LinkedHashMap() }
            // Keep first seen or smaller file for the same weight
            if (!variants.containsKey(weight)) {
                variants[weight] = file
            }
            if (!displayNames.containsKey(normalizedId)) {
                displayNames[normalizedId] = formatFamilyDisplayName(familyRaw)
            }
        }

        val discovered = grouped.mapNotNull { (id, variants) ->
            if (variants.isEmpty()) return@mapNotNull null
            val name = displayNames[id] ?: id.removePrefix("sys:")
            val weightsLabel = variants.keys.sorted().joinToString("/") { it.toString() }
            val sampleFile = variants[400] ?: variants.values.first()
            SystemFontEntry(
                id = id,
                displayName = name,
                subtitle = "${sampleFile.name} · Weights: $weightsLabel",
                isBuiltInLogical = false,
                variantFiles = variants.toMap()
            )
        }.sortedWith(
            compareByDescending<SystemFontEntry> { it.variantFiles.size > 1 }
                .thenBy { it.displayName.lowercase() }
        )

        val result = getLogicalPresets() + discovered
        cachedEntries = result
        return result
    }

    fun resolveFontFamily(fontId: String): FontFamily {
        when (fontId) {
            ID_SYSTEM_DEFAULT, "" -> return FontFamily.Default
            ID_SANS_SERIF -> return FontFamily.SansSerif
            ID_SERIF -> return FontFamily.Serif
            ID_MONOSPACE -> return FontFamily.Monospace
            ID_CURSIVE -> return FontFamily.Cursive
        }

        familyCache[fontId]?.let { return it }

        val entries = cachedEntries ?: scanSystemFonts()
        val entry = entries.firstOrNull { it.id == fontId } ?: return FontFamily.Default
        if (entry.variantFiles.isEmpty()) return FontFamily.Default

        return try {
            val composeFonts = entry.variantFiles.entries.mapNotNull { (weightVal, file) ->
                try {
                    if (file.exists() && file.canRead()) {
                        Font(file = file, weight = FontWeight(weightVal.coerceIn(100, 900)))
                    } else null
                } catch (_: Throwable) {
                    null
                }
            }
            val resolved = if (composeFonts.isNotEmpty()) {
                FontFamily(composeFonts)
            } else {
                FontFamily.Default
            }
            familyCache[fontId] = resolved
            resolved
        } catch (_: Throwable) {
            FontFamily.Default
        }
    }

    fun getDisplayName(fontId: String): String {
        val entries = cachedEntries ?: getLogicalPresets()
        return entries.firstOrNull { it.id == fontId }?.displayName
            ?: when (fontId) {
                ID_SYSTEM_DEFAULT, "" -> "System Default"
                ID_SANS_SERIF -> "Sans-Serif"
                ID_SERIF -> "Serif"
                ID_MONOSPACE -> "Monospace"
                ID_CURSIVE -> "Cursive"
                else -> fontId.removePrefix("sys:").replace('_', ' ')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun parseFamilyAndWeight(file: File, fileNameNoExt: String): Pair<String, Int> {
        val weightTokens = listOf(
            "extralight" to 200,
            "ultralight" to 200,
            "thin" to 100,
            "light" to 300,
            "demilight" to 300,
            "regular" to 400,
            "normal" to 400,
            "book" to 400,
            "medium" to 500,
            "semibold" to 600,
            "demibold" to 600,
            "extrabold" to 800,
            "ultrabold" to 800,
            "bold" to 700,
            "black" to 900,
            "heavy" to 900
        )

        var detectedWeight = 400
        val lower = fileNameNoExt.lowercase()
        // Ignore pure italic variants so upright text stays clean
        if (lower.endsWith("italic") && !lower.contains("regular")) {
            return "" to 400
        }

        for ((token, w) in weightTokens) {
            if (lower.endsWith("-$token") || lower.endsWith("_$token") || lower == token) {
                detectedWeight = w
                break
            }
        }

        val cleanedFamily = fileNameNoExt
            .replace(
                Regex("(?i)[-_](ExtraLight|UltraLight|Thin|DemiLight|Light|Regular|Normal|Book|Medium|SemiBold|DemiBold|ExtraBold|UltraBold|Bold|Black|Heavy|Italic|Oblique|VF|Variable).*$"),
                ""
            )
            .trim()

        // Try reading TTF/OTF name table for single-file fonts if filename is generic
        val ttfFamily = if (file.extension.equals("ttf", ignoreCase = true) ||
            file.extension.equals("otf", ignoreCase = true)
        ) {
            readTtfFamilyName(file)
        } else null

        val finalFamily = if (!ttfFamily.isNullOrBlank() && ttfFamily.length in 2..40) {
            ttfFamily
        } else {
            cleanedFamily
        }

        return finalFamily to detectedWeight
    }

    private fun formatFamilyDisplayName(raw: String): String {
        return raw
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Lightweight TrueType/OpenType 'name' table parser to extract Name ID 1 (Font Family Name).
     */
    private fun readTtfFamilyName(file: File): String? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 12) return null
                val sfntVersion = raf.readInt()
                // 0x00010000 for TrueType, 'OTTO' (0x4F54544F) for OpenType
                if (sfntVersion != 0x00010000 && sfntVersion != 0x4F54544F) return null
                val numTables = raf.readUnsignedShort()
                raf.skipBytes(6)

                var nameOffset = -1L
                for (i in 0 until numTables) {
                    val tagBytes = ByteArray(4)
                    raf.readFully(tagBytes)
                    val tag = String(tagBytes, Charsets.US_ASCII)
                    raf.skipBytes(4) // checkSum
                    val offset = raf.readInt().toLong() and 0xFFFFFFFFL
                    raf.skipBytes(4) // length
                    if (tag == "name") {
                        nameOffset = offset
                        break
                    }
                }
                if (nameOffset <= 0L || nameOffset >= raf.length()) return null

                raf.seek(nameOffset)
                raf.skipBytes(2) // format
                val count = raf.readUnsignedShort()
                val stringOffset = raf.readUnsignedShort()
                val storageStart = nameOffset + stringOffset

                var bestName: String? = null
                val maxRecords = count.coerceAtMost(64)
                for (i in 0 until maxRecords) {
                    val platformId = raf.readUnsignedShort()
                    val encodingId = raf.readUnsignedShort()
                    val languageId = raf.readUnsignedShort()
                    val nameId = raf.readUnsignedShort()
                    val length = raf.readUnsignedShort()
                    val offset = raf.readUnsignedShort()

                    // Name ID 1 = Font Family name, Name ID 16 = Typographic Family name
                    if ((nameId == 16 || nameId == 1) && length in 1..128) {
                        val pos = raf.filePointer
                        val strPos = storageStart + offset
                        if (strPos + length <= raf.length()) {
                            raf.seek(strPos)
                            val data = ByteArray(length)
                            raf.readFully(data)
                            val decoded = when (platformId) {
                                3, 0 -> String(data, Charset.forName("UTF-16BE"))
                                1 -> if (encodingId == 0) String(data, Charsets.US_ASCII) else null
                                else -> null
                            }?.trim { it <= ' ' || it == '\u0000' }

                            if (!decoded.isNullOrBlank() && !decoded.startsWith(".")) {
                                if (nameId == 16 || bestName == null || languageId == 0x0804 || languageId == 0x0409) {
                                    bestName = decoded
                                    if (nameId == 16) {
                                        raf.seek(pos)
                                        break
                                    }
                                }
                            }
                        }
                        raf.seek(pos)
                    }
                }
                bestName
            }
        } catch (_: Throwable) {
            null
        }
    }
}
