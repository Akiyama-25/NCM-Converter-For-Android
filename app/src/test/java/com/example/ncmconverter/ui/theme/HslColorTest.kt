package com.example.ncmconverter.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class HslColorTest {

    private fun assertArgbU(expected: UInt, h: Float, s: Float, l: Float) {
        val actual = hslToArgbLong(h, s, l).toUInt()
        assertEquals("HSL($h, $s, $l)", expected, actual)
    }

    // --- Primary colors ---

    @Test
    fun `pure red HSL 0 100 50`() {
        assertArgbU(0xFFFF0000u, 0f, 100f, 50f)
    }

    @Test
    fun `pure green HSL 120 100 50`() {
        assertArgbU(0xFF00FF00u, 120f, 100f, 50f)
    }

    @Test
    fun `pure blue HSL 240 100 50`() {
        assertArgbU(0xFF0000FFu, 240f, 100f, 50f)
    }

    // --- Black / White / Gray ---

    @Test
    fun `black HSL 0 0 0`() {
        assertArgbU(0xFF000000u, 0f, 0f, 0f)
    }

    @Test
    fun `white HSL 0 0 100`() {
        assertArgbU(0xFFFFFFFFu, 0f, 0f, 100f)
    }

    @Test
    fun `mid gray HSL 0 0 50`() {
        assertArgbU(0xFF808080u, 0f, 0f, 50f)
    }

    // --- Hue wrapping ---

    @Test
    fun `hue 360 wraps to 0 (red)`() {
        val h360 = hslToArgbLong(360f, 100f, 50f).toUInt()
        val h0 = hslToArgbLong(0f, 100f, 50f).toUInt()
        assertEquals(h0, h360)
    }

    @Test
    fun `hue -120 wraps to 240 (blue)`() {
        val negative = hslToArgbLong(-120f, 100f, 50f).toUInt()
        val positive = hslToArgbLong(240f, 100f, 50f).toUInt()
        assertEquals(positive, negative)
    }

    // --- Clamping ---

    @Test
    fun `saturation clamped above 100`() {
        val clamped = hslToArgbLong(0f, 150f, 50f).toUInt()
        val normal = hslToArgbLong(0f, 100f, 50f).toUInt()
        assertEquals(normal, clamped)
    }

    @Test
    fun `saturation clamped below 0`() {
        val clamped = hslToArgbLong(0f, -10f, 50f).toUInt()
        val normal = hslToArgbLong(0f, 0f, 50f).toUInt()
        assertEquals(normal, clamped)
    }

    @Test
    fun `lightness clamped above 100`() {
        val clamped = hslToArgbLong(120f, 50f, 120f).toUInt()
        val normal = hslToArgbLong(120f, 50f, 100f).toUInt()
        assertEquals(normal, clamped)
    }

    // --- Intermediate hues ---

    @Test
    fun `yellow HSL 60 100 50`() {
        assertArgbU(0xFFFFFF00u, 60f, 100f, 50f)
    }

    @Test
    fun `cyan HSL 180 100 50`() {
        assertArgbU(0xFF00FFFFu, 180f, 100f, 50f)
    }

    @Test
    fun `magenta HSL 300 100 50`() {
        assertArgbU(0xFFFF00FFu, 300f, 100f, 50f)
    }

    // --- Round-trip: hslToArgbLong -> Color -> colorToHsl ---

    @Test
    fun `round-trip red`() {
        val color = hslToColor(0f, 100f, 50f)
        val hsl = colorToHsl(color)
        assertEquals(0f, hsl.h, 1f)
        assertEquals(100f, hsl.s, 1f)
        assertEquals(50f, hsl.l, 1f)
    }

    @Test
    fun `round-trip blue`() {
        val color = hslToColor(240f, 100f, 50f)
        val hsl = colorToHsl(color)
        assertEquals(240f, hsl.h, 1f)
        assertEquals(100f, hsl.s, 1f)
        assertEquals(50f, hsl.l, 1f)
    }

    @Test
    fun `round-trip gray`() {
        val color = hslToColor(0f, 0f, 50f)
        val hsl = colorToHsl(color)
        assertEquals(0f, hsl.h, 1f)
        assertEquals(0f, hsl.s, 1f)
        assertEquals(50f, hsl.l, 1f)
    }

    @Test
    fun `round-trip arbitrary color`() {
        val h = 141f; val s = 73f; val l = 41f
        val color = hslToColor(h, s, l)
        val hsl = colorToHsl(color)
        assertEquals(h, hsl.h, 1f)
        assertEquals(s, hsl.s, 1f)
        assertEquals(l, hsl.l, 1f)
    }

    // --- hslToArgbLong returns correct ARGB format ---

    @Test
    fun `argb has full alpha for pure colors`() {
        val argb = hslToArgbLong(0f, 100f, 50f).toUInt()
        val alpha = (argb shr 24) and 0xFFu
        assertEquals(0xFFu, alpha)
    }
}
