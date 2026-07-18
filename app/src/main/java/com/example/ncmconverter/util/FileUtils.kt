package com.example.ncmconverter.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.ncmconverter.decrypt.model.DecryptResult
import com.example.ncmconverter.metadata.FlacMetadataWriter
import com.example.ncmconverter.metadata.Mp3MetadataWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object FileUtils {

    fun queryFileName(uri: Uri, context: Context): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx) ?: "unknown"
            }
        }
        return name
    }

    fun queryFileSize(uri: Uri, context: Context): Long {
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (idx >= 0) size = cursor.getLong(idx)
            }
        }
        return size
    }

    fun readBytes(uri: Uri, context: Context): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }

    suspend fun saveToCustomPath(
        context: Context,
        result: DecryptResult,
        relativePath: String,
        metadataWriter: suspend (ByteArray) -> ByteArray
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val displayName = buildDisplayName(result) + "." + result.extension
            val cleanPath = relativePath.trim('/')

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, result.mimeType)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "$cleanPath/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val outputUri = resolver.insert(collection, contentValues) ?: return@withContext null

            if (result.isFileBased && result.tempAudioFile != null) {
                resolver.openOutputStream(outputUri)?.use { output ->
                    if (result.extension == "flac") {
                        // FLAC: write metadata header then stream from byte 42 (skip original fLaC+STREAMINFO)
                        writeFlacMetadataHeader(output, result)
                        result.tempAudioFile.inputStream().use { input ->
                            input.skip(42) // skip "fLaC" (4) + STREAMINFO block header+data (38)
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    } else {
                        // MP3: write ID3 tag then stream-copy entire file (no existing tag to skip)
                        writeMp3MetadataHeader(output, result)
                        result.tempAudioFile.inputStream().use { input ->
                            input.copyTo(output, bufferSize = 64 * 1024)
                        }
                    }
                } ?: run {
                    try { resolver.delete(outputUri, null, null) } catch (_: Exception) {}
                    return@withContext null
                }
            } else {
                val audioBytes = result.audioData ?: return@withContext null
                val taggedAudio = metadataWriter(audioBytes)
                resolver.openOutputStream(outputUri)?.use { output ->
                    output.write(taggedAudio)
                } ?: run {
                    try { resolver.delete(outputUri, null, null) } catch (_: Exception) {}
                    return@withContext null
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(outputUri, contentValues, null, null)

            val verified = resolver.query(outputUri, arrayOf(MediaStore.Audio.Media.SIZE), null, null, null)?.use { cursor ->
                cursor.moveToFirst() && cursor.getLong(0) > 0
            } ?: false

            if (verified) outputUri else {
                try { resolver.delete(outputUri, null, null) } catch (_: Exception) {}
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Write MP3 ID3v2 tag header to the output stream (without audio data).
     */
    private suspend fun writeMp3MetadataHeader(output: OutputStream, result: DecryptResult) {
        val mp3Writer = Mp3MetadataWriter()
        val tagged = mp3Writer.write(ByteArray(0), result.metadata, result.lyric)
        output.write(tagged)
    }

    /**
     * Write FLAC metadata blocks to the output stream:
     * "fLaC" + adjusted STREAMINFO + Vorbis Comment + Picture blocks.
     * Audio data from byte 42 onward will be appended by the caller.
     */
    private suspend fun writeFlacMetadataHeader(output: OutputStream, result: DecryptResult) {
        val header = ByteArray(42)
        val read = result.tempAudioFile!!.inputStream().use { it.read(header) }
        if (read < 42 || header[0] != 0x66.toByte()) return

        val flacWriter = FlacMetadataWriter()
        val vorbisComment = flacWriter.buildVorbisComment(result.metadata, result.lyric)

        val coverBytes = if (AppPrefs.enableCover) CoverFetcher.fetch(result.metadata.albumPic) else null
        val hasPicture = coverBytes != null && coverBytes.isNotEmpty()
        val pictureBlock = if (hasPicture) {
            val picture = flacWriter.buildPictureBlock(coverBytes!!)
            flacWriter.buildMetadataBlockHeader(6, true, picture.size) + picture
        } else {
            byteArrayOf()
        }

        // Vorbis Comment is isLast=true only when there's no Picture block after it
        val vorbisHeader = flacWriter.buildMetadataBlockHeader(4, !hasPicture, vorbisComment.size)

        val streaminfoHeader = header[4].toInt() and 0xff
        val isLastBlock = (streaminfoHeader and 0x80) != 0
        val adjustedStreaminfoHeader = if (isLastBlock && (vorbisComment.isNotEmpty() || pictureBlock.isNotEmpty())) {
            (streaminfoHeader and 0x7f).toByte()
        } else {
            header[4]
        }

        output.write(header, 0, 4) // "fLaC"
        output.write(adjustedStreaminfoHeader.toInt())
        output.write(header, 5, 37) // 3 bytes size + 34 bytes STREAMINFO data

        if (vorbisComment.isNotEmpty()) output.write(vorbisHeader + vorbisComment)
        if (pictureBlock.isNotEmpty()) output.write(pictureBlock)
    }

    private fun buildDisplayName(result: DecryptResult): String {
        val meta = result.metadata
        val title = if (meta.musicName == "Unknown" || meta.musicName.isBlank()) "output" else meta.musicName
        val artists = meta.artists.joinToString(", ")
        val base = if (artists.isNotBlank()) "$artists - $title" else title
        return base.replace(Regex("[/\\\\:*?\"<>|]"), "_")
    }
}
