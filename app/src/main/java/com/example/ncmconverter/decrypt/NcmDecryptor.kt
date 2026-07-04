package com.example.ncmconverter.decrypt

import android.util.Base64
import com.example.ncmconverter.decrypt.model.DecryptProgress
import com.example.ncmconverter.decrypt.model.DecryptResult
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.decrypt.model.NcmMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import kotlin.math.min

class NcmDecryptor {

    companion object {
        private val CORE_KEY = hexToBytes("687a4852416d736f356b496e62617857")
        private val META_KEY = hexToBytes("2331346C6A6B5F215C5D2630553C2728")
        private const val STREAMING_THRESHOLD = 30L * 1024 * 1024 // 30MB

        private fun hexToBytes(hex: String): ByteArray {
            val result = ByteArray(hex.length / 2)
            for (i in hex.indices step 2) {
                result[i / 2] = hex.substring(i, i + 2).toInt(16).toByte()
            }
            return result
        }
    }

    private val _progress = MutableStateFlow(DecryptProgress())
    val progress: Flow<DecryptProgress> = _progress.asStateFlow()

    private fun emitProgress(state: DecryptState, progress: Float = 0f) {
        _progress.value = DecryptProgress(state = state, progress = progress)
    }

    /**
     * In-memory decryption for small files (< 30MB).
     */
    suspend fun decrypt(data: ByteArray, originalFilename: String): DecryptResult =
        withContext(Dispatchers.Default) {
            emitProgress(DecryptState.PARSING)

            val parser = NcmFileParser(data)
            if (!parser.verifyMagic()) {
                emitProgress(DecryptState.FAILED)
                throw IllegalArgumentException("不是有效的 NCM 文件：文件头校验失败")
            }

            val keyBlock = parser.readKeyBlock()
            val decryptedKeyBlock = AesEcbDecryptor.decrypt(keyBlock, CORE_KEY)
            val rc4Key = decryptedKeyBlock.copyOfRange(17, decryptedKeyBlock.size)
            val keyBox = Rc4Engine.buildKeyBox(rc4Key)

            val metaBlock = parser.readMetaBlock()
            val metadata = parseMetaBlock(metaBlock)

            emitProgress(DecryptState.DECRYPTING)
            val encryptedAudio = parser.getEncryptedAudioData()
            val audioLen = encryptedAudio.size
            val decryptedAudio = ByteArray(audioLen)

            val chunkSize = 64 * 1024
            var lastProgressReport = 0L
            for (offset in 0 until audioLen step chunkSize) {
                val end = min(offset + chunkSize, audioLen)
                for (i in offset until end) {
                    decryptedAudio[i] = (encryptedAudio[i].toInt() xor keyBox[i and 0xff]).toByte()
                }
                val processed = end.toLong()
                if (processed - lastProgressReport >= chunkSize * 4) {
                    lastProgressReport = processed
                    emitProgress(DecryptState.DECRYPTING, processed.toFloat() / audioLen)
                }
            }
            emitProgress(DecryptState.DECRYPTING, 1f)

            val (mimeType, extension) = detectFormat(decryptedAudio)
            parser.clearReference()

            DecryptResult(
                audioData = decryptedAudio,
                metadata = metadata,
                mimeType = mimeType,
                extension = extension
            )
        }

    /**
     * Streaming decryption for large files (>= 30MB).
     * Reads directly from InputStream, writes decrypted audio to a temp file.
     * Peak memory: ~128KB (buffer only), regardless of file size.
     */
    suspend fun decrypt(inputStream: InputStream, originalFilename: String): DecryptResult =
        withContext(Dispatchers.Default) {
            emitProgress(DecryptState.PARSING)

            // Verify magic header (8 bytes)
            val magic = ByteArray(8)
            readFully(inputStream, magic)
            val expectedMagic = byteArrayOf(67, 84, 69, 78, 70, 68, 65, 77) // "CTENFDAM"
            for (i in 0 until 8) {
                if (magic[i] != expectedMagic[i]) {
                    emitProgress(DecryptState.FAILED)
                    throw IllegalArgumentException("不是有效的 NCM 文件：文件头校验失败")
                }
            }

            // Skip 2 bytes (skip header)
            skipFully(inputStream, 2)

            // Read key block (XOR-decrypt with 0x64, then AES-ECB decrypt)
            val keyLength = readUint32LE(inputStream)
            val keyRaw = ByteArray(keyLength)
            readFully(inputStream, keyRaw)
            val keyEncrypted = ByteArray(keyLength)
            for (i in 0 until keyLength) {
                keyEncrypted[i] = (keyRaw[i].toInt() xor 0x64).toByte()
            }
            val decryptedKeyBlock = AesEcbDecryptor.decrypt(keyEncrypted, CORE_KEY)
            val rc4Key = decryptedKeyBlock.copyOfRange(17, decryptedKeyBlock.size)
            val keyBox = Rc4Engine.buildKeyBox(rc4Key)

            // Read meta block
            val metaLength = readUint32LE(inputStream)
            val metaEncrypted = ByteArray(metaLength)
            readFully(inputStream, metaEncrypted)
            val metaDecrypted = ByteArray(metaLength)
            for (i in 0 until metaLength) {
                metaDecrypted[i] = (metaEncrypted[i].toInt() xor 0x63).toByte()
            }
            val metadata = if (metaDecrypted.size > 22) {
                val metaPayload = metaDecrypted.copyOfRange(22, metaDecrypted.size)
                val decoded = Base64.decode(metaPayload, Base64.DEFAULT)
                val decryptedJson = AesEcbDecryptor.decrypt(decoded, META_KEY)
                parseMetadata(String(decryptedJson))
            } else {
                NcmMetadata()
            }

            // Skip gap block:
            // [5 bytes type] [4 bytes gapSize (uint32 LE)] [gapSize bytes data] [4 bytes padding]
            // Original parser reads gapSize at offset + 5, so skip the 5-byte prefix first
            skipFully(inputStream, 5) // skip 5 bytes before the gap size field
            val gapSize = readUint32LE(inputStream) // read gap size
            skipFully(inputStream, gapSize.toLong() + 4) // skip gap data + 4 trailing bytes

            // Decrypt remaining audio data directly to temp file (streaming I/O)
            emitProgress(DecryptState.DECRYPTING)
            val tempFile = withContext(Dispatchers.IO) {
                File.createTempFile("ncm_decrypt_", ".tmp")
            }
            tempFile.deleteOnExit()

            var firstBytes: ByteArray? = null
            val tempOut = tempFile.outputStream()
            try {
                val bufSize = 64 * 1024 // 64KB buffer
                val readBuf = ByteArray(bufSize)
                val writeBuf = ByteArray(bufSize)
                var totalProcessed = 0L
                var lastProgressReport = 0L

                while (true) {
                    val bytesRead = inputStream.read(readBuf)
                    if (bytesRead <= 0) break

                    for (i in 0 until bytesRead) {
                        writeBuf[i] = (readBuf[i].toInt() xor keyBox[(totalProcessed + i).toInt() and 0xff]).toByte()
                    }
                    tempOut.write(writeBuf, 0, bytesRead)

                    if (firstBytes == null) {
                        firstBytes = writeBuf.copyOf(min(bytesRead, 4))
                    }

                    totalProcessed += bytesRead
                    if (totalProcessed - lastProgressReport >= bufSize * 4) {
                        lastProgressReport = totalProcessed
                        emitProgress(DecryptState.DECRYPTING, 0f) // progress unknown without file size
                    }
                }
            } finally {
                tempOut.close()
            }
            emitProgress(DecryptState.DECRYPTING, 1f)

            val (mimeType, extension) = detectFormat(firstBytes ?: ByteArray(0))

            DecryptResult(
                tempAudioFile = tempFile,
                metadata = metadata,
                mimeType = mimeType,
                extension = extension
            )
        }

    private fun readFully(input: InputStream, buf: ByteArray) {
        var offset = 0
        while (offset < buf.size) {
            val read = input.read(buf, offset, buf.size - offset)
            if (read == -1) throw IllegalArgumentException("意外的流结束")
            offset += read
        }
    }

    private fun skipFully(input: InputStream, n: Long) {
        var remaining = n
        val buf = ByteArray(8192)
        while (remaining > 0) {
            val toSkip = min(remaining, buf.size.toLong()).toInt()
            val skipped = input.read(buf, 0, toSkip)
            if (skipped == -1) throw IllegalArgumentException("意外的流结束")
            remaining -= skipped
        }
    }

    private fun readUint32LE(input: InputStream): Int {
        val b = ByteArray(4)
        readFully(input, b)
        return (b[0].toInt() and 0xff) or
                ((b[1].toInt() and 0xff) shl 8) or
                ((b[2].toInt() and 0xff) shl 16) or
                ((b[3].toInt() and 0xff) shl 24)
    }

    private fun parseMetaBlock(metaBlock: ByteArray): NcmMetadata {
        return if (metaBlock.size > 22) {
            val metaPayload = metaBlock.copyOfRange(22, metaBlock.size)
            val decoded = Base64.decode(metaPayload, Base64.DEFAULT)
            val decryptedJson = AesEcbDecryptor.decrypt(decoded, META_KEY)
            parseMetadata(String(decryptedJson))
        } else {
            NcmMetadata()
        }
    }

    private fun parseMetadata(jsonStr: String): NcmMetadata {
        return try {
            val cleanJson = jsonStr.substringAfter(":").trim()
            val json = JSONObject(cleanJson)
            val metaJson = if (jsonStr.startsWith("dj:")) {
                json.optJSONObject("mainMusic") ?: json
            } else {
                json
            }

            val artists = mutableListOf<String>()
            metaJson.optJSONArray("artist")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.get(i)
                    val name = when (item) {
                        is JSONArray -> item.optString(0) ?: ""
                        is String -> item
                        else -> item.toString()
                    }
                    if (name.isNotBlank()) artists.add(name)
                }
            }

            var albumPic = metaJson.optString("albumPic", "")
            if (albumPic.isNotBlank()) {
                albumPic = albumPic
                    .replace("http://", "https://")
                    .trim()
                val separator = if ("?" in albumPic) "&" else "?"
                albumPic += "${separator}param=500y500"
            }

            NcmMetadata(
                musicId = metaJson.optLong("musicId", 0L),
                musicName = metaJson.optString("musicName", "Unknown"),
                artists = artists,
                album = metaJson.optString("album", ""),
                albumPic = albumPic,
                format = metaJson.optString("format", ""),
                duration = metaJson.optLong("duration", 0)
            )
        } catch (e: Exception) {
            NcmMetadata()
        }
    }

    private fun detectFormat(data: ByteArray): Pair<String, String> {
        if (data.size < 4) return "audio/mpeg" to "mp3"
        if (data[0] == 0x66.toByte() && data[1] == 0x4c.toByte() &&
            data[2] == 0x61.toByte() && data[3] == 0x43.toByte()
        ) {
            return "audio/flac" to "flac"
        }
        if ((data[0] == 0xff.toByte() && (data[1].toInt() and 0xe0) == 0xe0) ||
            (data[0] == 0x49.toByte() && data[1] == 0x44.toByte() && data[2] == 0x33.toByte())
        ) {
            return "audio/mpeg" to "mp3"
        }
        return "audio/mpeg" to "mp3"
    }
}
