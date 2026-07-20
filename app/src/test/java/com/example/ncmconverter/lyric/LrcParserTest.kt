package com.example.ncmconverter.lyric

import com.example.ncmconverter.api.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    // --- parse ---

    @Test
    fun `parse null returns empty`() {
        assertTrue(LrcParser.parse(null).isEmpty())
    }

    @Test
    fun `parse blank string returns empty`() {
        assertTrue(LrcParser.parse("   ").isEmpty())
    }

    @Test
    fun `parse single line with 2-digit millis`() {
        val lines = LrcParser.parse("[00:12.34]Hello World")
        assertEquals(1, lines.size)
        assertEquals(12340L, lines[0].timestamp)
        assertEquals("Hello World", lines[0].text)
    }

    @Test
    fun `parse single line with 3-digit millis`() {
        val lines = LrcParser.parse("[01:30.500]Test lyric")
        assertEquals(1, lines.size)
        assertEquals(90500L, lines[0].timestamp)
        assertEquals("Test lyric", lines[0].text)
    }

    @Test
    fun `parse multiple lines sorted by timestamp`() {
        val lrc = """
            [00:30.00]Third
            [00:10.00]First
            [00:20.00]Second
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(3, lines.size)
        assertEquals("First", lines[0].text)
        assertEquals("Second", lines[1].text)
        assertEquals("Third", lines[2].text)
    }

    @Test
    fun `parse skips invalid lines`() {
        val lrc = """
            [00:10.00]Valid
            Invalid line
            [00:20.00]Also valid
        """.trimIndent()
        val lines = LrcParser.parse(lrc)
        assertEquals(2, lines.size)
    }

    @Test
    fun `parse empty text after timestamp`() {
        val lines = LrcParser.parse("[00:05.00]")
        assertEquals(1, lines.size)
        assertEquals("", lines[0].text)
    }

    @Test
    fun `parse 2-digit millis pads to 3 digits`() {
        // [00:00.50] should be 500ms (padded to .500)
        val lines = LrcParser.parse("[00:00.50]pad test")
        assertEquals(500L, lines[0].timestamp)
    }

    // --- toLrcString ---

    @Test
    fun `toLrcString format`() {
        val lines = listOf(LyricLine(90500L, "Hello"))
        val result = LrcParser.toLrcString(lines)
        assertEquals("[01:30.500]Hello", result)
    }

    @Test
    fun `toLrcString multiple lines`() {
        val lines = listOf(
            LyricLine(10000L, "Line 1"),
            LyricLine(20000L, "Line 2")
        )
        val result = LrcParser.toLrcString(lines)
        assertEquals("[00:10.000]Line 1\n[00:20.000]Line 2", result)
    }

    // --- merge ---

    @Test
    fun `merge empty original returns empty`() {
        val result = LrcParser.merge(emptyList(), listOf(LyricLine(1000L, "trans")))
        assertEquals("", result)
    }

    @Test
    fun `merge empty translated returns original as lrc`() {
        val original = listOf(LyricLine(10000L, "Hello"))
        val result = LrcParser.merge(original, emptyList())
        assertEquals("[00:10.000]Hello", result)
    }

    @Test
    fun `merge matching timestamps`() {
        val original = listOf(LyricLine(10000L, "Hello"))
        val translated = listOf(LyricLine(10000L, "你好"))
        val result = LrcParser.merge(original, translated)
        assertTrue(result.contains("Hello"))
        assertTrue(result.contains("你好"))
    }

    @Test
    fun `merge unmatched timestamps keeps original only`() {
        val original = listOf(
            LyricLine(10000L, "Line 1"),
            LyricLine(20000L, "Line 2")
        )
        val translated = listOf(LyricLine(10000L, "翻译1"))
        val result = LrcParser.merge(original, translated)
        // Line 1 should have translation, Line 2 should not
        assertTrue(result.contains("翻译1"))
        assertTrue(result.contains("Line 2"))
    }

    @Test
    fun `merge ignores blank translated text`() {
        val original = listOf(LyricLine(10000L, "Hello"))
        val translated = listOf(LyricLine(10000L, "   "))
        val result = LrcParser.merge(original, translated)
        // Should not contain blank translation
        assertEquals("[00:10.000]Hello", result)
    }

    // --- LyricLine.toLrcString ---

    @Test
    fun `LyricLine toLrcString zero timestamp`() {
        val line = LyricLine(0L, "Start")
        assertEquals("[00:00.000]Start", line.toLrcString())
    }

    @Test
    fun `LyricLine toLrcString large timestamp`() {
        // 3 minutes 25 seconds 123 millis = 205123ms
        val line = LyricLine(205123L, "End")
        assertEquals("[03:25.123]End", line.toLrcString())
    }
}
