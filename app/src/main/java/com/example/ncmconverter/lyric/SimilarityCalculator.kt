package com.example.ncmconverter.lyric

object SimilarityCalculator {

    private val CHAR_REPLACEMENTS = mapOf(
        "（" to "(", "）" to ")",  // （ ）
        "【" to "[", "】" to "]",  // 【 】
        "，" to ",", "。" to ".",  // ， 。
        "！" to "!", "？" to "?",  // ！ ？
        "：" to ":", "；" to ";",  // ： ；
        "“" to "\"", "”" to "\"", // " "
        "‘" to "'", "’" to "'",  // ' '
        "　" to " ",                    // ideographic space
        "＆" to "&", "＊" to "*",  // ＆ ＊
        "＋" to "+", "＝" to "=",  // ＋ ＝
        "＜" to "<", "＞" to ">",  // ＜ ＞
        "～" to "~",                    // ～
        "①" to "1", "②" to "2", "③" to "3", "④" to "4", "⑤" to "5", // ①-⑤
        "Ⅰ" to "I", "Ⅱ" to "II", "Ⅲ" to "III" // Ⅰ Ⅱ Ⅲ
    )

    private val EXTRA_INFO_PATTERN = Regex("""\s*[\(（\[【].*?[\)）\]】]\s*$""")
    private val COVER_PATTERN = Regex("""(?i)\s*(?:cover|翻唱|改编|remix)\b.*$""")

    fun normalize(text: String): String {
        var result = text.trim()
        for ((from, to) in CHAR_REPLACEMENTS) {
            result = result.replace(from, to)
        }
        result = result.replace(Regex("""\s+"""), " ").trim()
        return result.lowercase()
    }

    fun stripExtraInfo(text: String): String {
        return text
            .replace(EXTRA_INFO_PATTERN, "")
            .replace(COVER_PATTERN, "")
            .trim()
    }

    fun calculateSimilarity(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val normA = normalize(a)
        val normB = normalize(b)

        if (normA == normB) return 1.0

        val distance = levenshteinDistance(normA, normB)
        val maxLen = maxOf(normA.length, normB.length)
        return 1.0 - distance.toDouble() / maxLen
    }

    fun calculateMatchScore(targetName: String, targetArtists: List<String>,
                           resultName: String, resultArtists: List<String>): Double {
        val nameScore = calculateSimilarity(targetName, resultName)
        val artistScore = if (targetArtists.isEmpty() || resultArtists.isEmpty()) {
            0.5
        } else {
            val targetSet = targetArtists.map { normalize(it) }.toSet()
            val resultSet = resultArtists.map { normalize(it) }.toSet()
            val intersection = targetSet.intersect(resultSet).size
            val union = targetSet.union(resultSet).size
            if (union == 0) 0.5 else intersection.toDouble() / union
        }

        return nameScore * 0.65 + artistScore * 0.35
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1)
                }
            }
        }
        return dp[m][n]
    }
}
