package com.example.ncmconverter.decrypt

object Rc4Engine {

    /**
     * Builds the 256-byte keyBox from the RC4 key.
     * Mirrors the JS _getKeyBox() exactly:
     *   1. Standard RC4 KSA to shuffle the identity permutation
     *   2. Map each position through a lookup transform to produce the final box
     */
    fun buildKeyBox(key: ByteArray): IntArray {
        // Step 1 — RC4 KSA: shuffle s[0..255] with the key
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xff)) and 0xff
            val tmp = s[i]
            s[i] = s[j]
            s[j] = tmp
        }

        // Step 2 — lookup transform: for each index t, compute s[ s[t+1] + s[(t+1 + s[t+1])&255] ]
        return IntArray(256) { t ->
            val a = (t + 1) and 0xff
            val sa = s[a]
            s[(sa + s[(a + sa) and 0xff]) and 0xff]
        }
    }
}
