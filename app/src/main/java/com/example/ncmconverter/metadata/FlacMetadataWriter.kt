package com.example.ncmconverter.metadata

import com.example.ncmconverter.decrypt.model.NcmMetadata
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FlacMetadataWriter : MetadataWriter {

    override suspend fun write(audioData: ByteArray, metadata: NcmMetadata): ByteArray {
        return write(audioData, metadata, null)
    }

    override suspend fun write(audioData: ByteArray, metadata: NcmMetadata, lyric: String?): ByteArray =
        withContext(Dispatchers.Default) {
            if (audioData.size < 42) return@withContext audioData // too small, skip

            // Find the end of the mandatory STREAMINFO block
            val streaminfoHeader = audioData[4].toInt() and 0xff
            val isLastBlock = (streaminfoHeader and 0x80) != 0
            val blockSize = ((audioData[5].toInt() and 0xff) shl 16) or
                    ((audioData[6].toInt() and 0xff) shl 8) or
                    (audioData[7].toInt() and 0xff)
            val streaminfoEnd = 4 + 4 + blockSize // 4=fLaC + 4=header + blockSize

            // Fetch cover art if enabled
            val coverBytes = if (AppPrefs.enableCover) fetchCoverArt(metadata.albumPic) else null

            // Build Vorbis Comment block
            val vorbisComment = buildVorbisComment(metadata, lyric)

            // Build Picture block (if cover art available)
            val hasPicture = coverBytes != null && coverBytes.isNotEmpty()
            val pictureBlock = if (hasPicture) {
                val picture = buildPictureBlock(coverBytes!!)
                buildMetadataBlockHeader(6, true, picture.size) + picture
            } else {
                byteArrayOf()
            }

            // Vorbis Comment is isLast=true only when there's no Picture block after it
            val vorbisHeader = buildMetadataBlockHeader(4, !hasPicture, vorbisComment.size)

            // Adjust STREAMINFO header if it was the last block
            val adjustedStreaminfoHeader = if (isLastBlock && (vorbisComment.isNotEmpty() || pictureBlock.isNotEmpty())) {
                // Clear the last-block bit
                (streaminfoHeader and 0x7f).toByte()
            } else {
                audioData[4]
            }

            // Reconstruct: fLaC + adjustedStreaminfoHeader + size + streaminfo data + our blocks + rest
            val out = ByteArrayOutputStream()
            out.write(audioData, 0, 4) // "fLaC"
            out.write(adjustedStreaminfoHeader.toInt())
            out.write(audioData, 5, blockSize + 3) // STREAMINFO size bytes + data
            if (vorbisComment.isNotEmpty()) out.write(vorbisHeader + vorbisComment)
            if (pictureBlock.isNotEmpty()) out.write(pictureBlock)
            out.write(audioData, streaminfoEnd, audioData.size - streaminfoEnd)
            out.toByteArray()
        }

    fun buildMetadataBlockHeader(type: Int, isLast: Boolean, size: Int): ByteArray {
        val header = (type and 0x7f) or (if (isLast) 0x80 else 0x00)
        return byteArrayOf(
            header.toByte(),
            ((size shr 16) and 0xff).toByte(),
            ((size shr 8) and 0xff).toByte(),
            (size and 0xff).toByte()
        )
    }

    fun buildVorbisComment(meta: NcmMetadata, lyric: String? = null): ByteArray {
        val vendor = "NCM Converter"
        val vendorBytes = vendor.toByteArray(Charsets.UTF_8)

        val comments = mutableListOf<String>()
        if (meta.musicName.isNotBlank() && meta.musicName != "Unknown") {
            comments.add("TITLE=${meta.musicName}")
        }
        if (meta.artists.isNotEmpty()) {
            comments.add("ARTIST=${meta.artists.joinToString("/")}")
        }
        if (meta.album.isNotBlank()) {
            comments.add("ALBUM=${meta.album}")
        }
        if (!lyric.isNullOrBlank()) {
            comments.add("LYRICS=$lyric")
        }

        val out = ByteArrayOutputStream()
        // Vendor string
        writeInt32LE(out, vendorBytes.size)
        out.write(vendorBytes)
        // Comments
        writeInt32LE(out, comments.size)
        for (comment in comments) {
            val bytes = comment.toByteArray(Charsets.UTF_8)
            writeInt32LE(out, bytes.size)
            out.write(bytes)
        }
        return out.toByteArray()
    }

    fun buildPictureBlock(cover: ByteArray): ByteArray {
        val mime = "image/jpeg"
        val mimeBytes = mime.toByteArray(Charsets.US_ASCII)
        val descBytes = "".toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        // Picture type: 3 = front cover
        writeInt32BE(out, 3)
        // MIME type
        writeInt32BE(out, mimeBytes.size)
        out.write(mimeBytes)
        // Description
        writeInt32BE(out, descBytes.size)
        out.write(descBytes)
        // Dimensions (0 = unknown)
        writeInt32BE(out, 0) // width
        writeInt32BE(out, 0) // height
        writeInt32BE(out, 24) // color depth
        writeInt32BE(out, 0) // number of colors
        // Picture data
        writeInt32BE(out, cover.size)
        out.write(cover)
        return out.toByteArray()
    }

    private fun writeInt32BE(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xff)
        out.write((value shr 16) and 0xff)
        out.write((value shr 8) and 0xff)
        out.write(value and 0xff)
    }

    private fun writeInt32LE(out: ByteArrayOutputStream, value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
        out.write((value shr 16) and 0xff)
        out.write((value shr 24) and 0xff)
    }

    suspend fun fetchCoverArt(url: String): ByteArray? {
        if (url.isBlank()) {
            android.util.Log.d("CoverArt", "FLAC albumPic is blank, skip")
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("CoverArt", "FLAC fetching cover: $url")
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.connect()
                android.util.Log.d("CoverArt", "FLAC response: ${connection.responseCode}")
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    android.util.Log.d("CoverArt", "FLAC cover downloaded: ${bytes.size} bytes")
                    bytes
                } else null
            } catch (e: Exception) {
                android.util.Log.e("CoverArt", "FLAC failed to fetch cover", e)
                null
            }
        }
    }
}
