package com.example.ncmconverter.api.model

data class LyricLine(
    val timestamp: Long,
    val text: String
) {
    fun toLrcString(): String {
        val minutes = timestamp / 60000
        val seconds = (timestamp % 60000) / 1000
        val millis = timestamp % 1000
        return String.format("[%02d:%02d.%03d]%s", minutes, seconds, millis, text)
    }
}

data class ParsedLyrics(
    val raw: String,
    val translated: String?,
    val merged: String
)
