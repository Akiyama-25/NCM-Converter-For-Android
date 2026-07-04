package com.example.ncmconverter.decrypt

class NcmFileParser(private var data: ByteArray?) {

    companion object {
        val MAGIC = byteArrayOf(67, 84, 69, 78, 70, 68, 65, 77) // "CTENFDAM"
        const val KEY_XOR: Byte = 0x64.toByte()   // 100
        const val META_XOR: Byte = 0x63.toByte()  // 99
    }

    // Sequential offset tracker, exactly matching JS `this.offset`
    private var offset = 10

    fun clearReference() {
        data = null
    }

    fun verifyMagic(): Boolean {
        val d = data ?: return false
        if (d.size < 8) return false
        for (i in 0 until 8) {
            if (d[i] != MAGIC[i]) return false
        }
        return true
    }

    fun readKeyBlock(): ByteArray {
        val d = data!!
        val keyLength = getUint32LE(offset)
        offset += 4
        val decrypted = ByteArray(keyLength)
        for (i in 0 until keyLength) {
            decrypted[i] = (d[offset + i].toInt() xor KEY_XOR.toInt()).toByte()
        }
        offset += keyLength
        return decrypted
    }

    fun readMetaBlock(): ByteArray {
        val d = data!!
        val metaLength = getUint32LE(offset)
        offset += 4
        val decrypted = ByteArray(metaLength)
        for (i in 0 until metaLength) {
            decrypted[i] = (d[offset + i].toInt() xor META_XOR.toInt()).toByte()
        }
        offset += metaLength
        return decrypted
    }

    fun getEncryptedAudioData(): ByteArray {
        val d = data!!
        // JS: this.offset += this.view.getUint32(this.offset + 5, true) + 13
        val v = getUint32LE(offset + 5)
        offset += v + 13
        return d.copyOfRange(offset, d.size)
    }

    private fun getUint32LE(pos: Int): Int {
        val d = data!!
        return (d[pos].toInt() and 0xff) or
                ((d[pos + 1].toInt() and 0xff) shl 8) or
                ((d[pos + 2].toInt() and 0xff) shl 16) or
                ((d[pos + 3].toInt() and 0xff) shl 24)
    }
}
