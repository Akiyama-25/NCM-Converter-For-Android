package com.example.ncmconverter.lyric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityCalculatorTest {

    // --- normalize ---

    @Test
    fun `normalize lowercases`() {
        assertEquals("hello", SimilarityCalculator.normalize("Hello"))
    }

    @Test
    fun `normalize full-width to half-width`() {
        assertEquals("(test)", SimilarityCalculator.normalize("（test）"))
    }

    @Test
    fun `normalize collapses whitespace`() {
        assertEquals("hello world", SimilarityCalculator.normalize("  hello   world  "))
    }

    @Test
    fun `normalize ideographic space`() {
        assertEquals("a b", SimilarityCalculator.normalize("a　b"))
    }

    @Test
    fun `normalize chinese punctuation`() {
        assertEquals("hello,world!", SimilarityCalculator.normalize("hello，world！"))
    }

    // --- stripExtraInfo ---

    @Test
    fun `stripExtraInfo removes parenthesized suffix`() {
        assertEquals("Song Name", SimilarityCalculator.stripExtraInfo("Song Name (Live)"))
    }

    @Test
    fun `stripExtraInfo removes bracketed suffix`() {
        assertEquals("Song", SimilarityCalculator.stripExtraInfo("Song [Remix]"))
    }

    @Test
    fun `stripExtraInfo removes cover suffix`() {
        assertEquals("Song", SimilarityCalculator.stripExtraInfo("Song Cover"))
    }

    @Test
    fun `stripExtraInfo removes chinese parenthesized suffix`() {
        assertEquals("歌曲", SimilarityCalculator.stripExtraInfo("歌曲（Live版）"))
    }

    @Test
    fun `stripExtraInfo no change when no suffix`() {
        assertEquals("Normal Song", SimilarityCalculator.stripExtraInfo("Normal Song"))
    }

    // --- calculateSimilarity ---

    @Test
    fun `identical strings return 1`() {
        assertEquals(1.0, SimilarityCalculator.calculateSimilarity("hello", "hello"), 0.001)
    }

    @Test
    fun `both empty return 1`() {
        assertEquals(1.0, SimilarityCalculator.calculateSimilarity("", ""), 0.001)
    }

    @Test
    fun `one empty return 0`() {
        assertEquals(0.0, SimilarityCalculator.calculateSimilarity("hello", ""), 0.001)
    }

    @Test
    fun `case insensitive`() {
        assertEquals(1.0, SimilarityCalculator.calculateSimilarity("Hello", "hello"), 0.001)
    }

    @Test
    fun `full-width equals half-width`() {
        assertEquals(1.0, SimilarityCalculator.calculateSimilarity("（test）", "(test)"), 0.001)
    }

    @Test
    fun `completely different strings near 0`() {
        val score = SimilarityCalculator.calculateSimilarity("abc", "xyz")
        assertTrue("Expected < 0.5, got $score", score < 0.5)
    }

    @Test
    fun `one char difference`() {
        val score = SimilarityCalculator.calculateSimilarity("hello", "hallo")
        assertTrue("Expected > 0.7, got $score", score > 0.7)
    }

    // --- calculateMatchScore ---

    @Test
    fun `exact match returns 1`() {
        val score = SimilarityCalculator.calculateMatchScore(
            "Song", listOf("Artist"),
            "Song", listOf("Artist")
        )
        assertEquals(1.0, score, 0.001)
    }

    @Test
    fun `name weight 0_65 artist weight 0_35`() {
        // Same name, different artist
        val score = SimilarityCalculator.calculateMatchScore(
            "Same", listOf("A"),
            "Same", listOf("B")
        )
        // nameScore=1.0, artistScore=0 (no intersection)
        // 1.0*0.65 + 0*0.35 = 0.65
        assertEquals(0.65, score, 0.01)
    }

    @Test
    fun `empty artist list gets 0_5 default`() {
        val score = SimilarityCalculator.calculateMatchScore(
            "Same", emptyList(),
            "Same", emptyList()
        )
        // nameScore=1.0, artistScore=0.5 (default)
        // 1.0*0.65 + 0.5*0.35 = 0.825
        assertEquals(0.825, score, 0.01)
    }

    @Test
    fun `partial artist overlap`() {
        val score = SimilarityCalculator.calculateMatchScore(
            "Song", listOf("A", "B"),
            "Song", listOf("B", "C")
        )
        // nameScore=1.0, artistScore: intersection=1(B), union=3(A,B,C) => 1/3
        // 1.0*0.65 + (1/3)*0.35 ≈ 0.767
        assertTrue("Expected ~0.767, got $score", score > 0.75 && score < 0.78)
    }

    @Test
    fun `multiple artists full overlap`() {
        val score = SimilarityCalculator.calculateMatchScore(
            "Song", listOf("A", "B"),
            "Song", listOf("A", "B")
        )
        assertEquals(1.0, score, 0.001)
    }
}
