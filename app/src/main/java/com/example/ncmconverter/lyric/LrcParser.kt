package com.example.ncmconverter.lyric

import com.example.ncmconverter.api.model.LyricLine

object LrcParser {

    private val TIMESTAMP_PATTERN = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")

    fun parse(lrcText: String?): List<LyricLine> {
        if (lrcText.isNullOrBlank()) return emptyList()

        return lrcText.lines()
            .mapNotNull { line -> parseLine(line) }
            .sortedBy { it.timestamp }
    }

    fun merge(original: List<LyricLine>, translated: List<LyricLine>): String {
        if (original.isEmpty()) return ""
        if (translated.isEmpty()) return toLrcString(original)

        val transMap = translated.associateBy { it.timestamp }
        val merged = mutableListOf<LyricLine>()

        for (line in original) {
            val trans = transMap[line.timestamp]
            if (trans != null && trans.text.isNotBlank()) {
                merged.add(LyricLine(line.timestamp, "${line.text}\n${line.timestamp.toLrcTimestamp()}${trans.text}"))
            } else {
                merged.add(line)
            }
        }

        return merged.joinToString("\n") { it.toLrcString() }
    }

    fun toLrcString(lines: List<LyricLine>): String {
        return lines.joinToString("\n") { it.toLrcString() }
    }

    private fun parseLine(line: String): LyricLine? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        val match = TIMESTAMP_PATTERN.matchEntire(trimmed) ?: return null
        val (min, sec, ms, text) = match.destructured

        val minutes = min.toLongOrNull() ?: return null
        val seconds = sec.toLongOrNull() ?: return null
        val millisPart = ms.padEnd(3, '0')
        val millis = millisPart.toLongOrNull() ?: return null

        val timestamp = minutes * 60000 + seconds * 1000 + millis
        return LyricLine(timestamp, text.trim())
    }

    private fun Long.toLrcTimestamp(): String {
        val minutes = this / 60000
        val seconds = (this % 60000) / 1000
        val millis = this % 1000
        return String.format("[%02d:%02d.%03d]", minutes, seconds, millis)
    }
}
