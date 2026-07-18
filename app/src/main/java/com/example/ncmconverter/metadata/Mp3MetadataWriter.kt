package com.example.ncmconverter.metadata

import com.example.ncmconverter.decrypt.model.NcmMetadata
import com.example.ncmconverter.util.AppPrefs
import com.example.ncmconverter.util.CoverFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class Mp3MetadataWriter : MetadataWriter {

    override suspend fun write(audioData: ByteArray, metadata: NcmMetadata): ByteArray {
        return write(audioData, metadata, null)
    }

    override suspend fun write(audioData: ByteArray, metadata: NcmMetadata, lyric: String?): ByteArray =
        withContext(Dispatchers.Default) {
            // Strip existing ID3v2 tag if present
            val rawAudio = if (audioData.size > 3 &&
                audioData[0] == 0x49.toByte() &&
                audioData[1] == 0x44.toByte() &&
                audioData[2] == 0x33.toByte()
            ) {
                val tagSize = readSynchSafeInt(audioData, 6)
                audioData.copyOfRange(10 + tagSize, audioData.size)
            } else {
                audioData
            }

            // Fetch cover art if available and enabled
            val coverBytes = if (AppPrefs.enableCover) CoverFetcher.fetch(metadata.albumPic) else null

            // Build ID3v2.3 tag
            val tag = buildId3v2Tag(metadata, coverBytes, lyric)

            // Concatenate: [ID3v2 tag] + [audio data]
            val out = ByteArrayOutputStream()
            out.write(tag)
            out.write(rawAudio)
            out.toByteArray()
        }

    private fun buildId3v2Tag(meta: NcmMetadata, cover: ByteArray?, lyric: String? = null): ByteArray {
        val frames = ByteArrayOutputStream()

        // TIT2 — Title
        writeTextFrame(frames, "TIT2", meta.musicName)

        // TPE1 — Artist
        val artist = meta.artists.joinToString("/")
        if (artist.isNotBlank()) {
            writeTextFrame(frames, "TPE1", artist)
        }

        // TALB — Album
        if (meta.album.isNotBlank()) {
            writeTextFrame(frames, "TALB", meta.album)
        }

        // APIC — Attached Picture
        if (cover != null && cover.isNotEmpty()) {
            writeApicFrame(frames, cover)
        }

        // USLT — Unsynchronized Lyrics
        if (!lyric.isNullOrBlank()) {
            writeUsltFrame(frames, lyric)
        }

        val frameBytes = frames.toByteArray()
        val header = ByteArray(10)
        header[0] = 0x49 // I
        header[1] = 0x44 // D
        header[2] = 0x33 // 3
        header[3] = 0x03 // version 2.3
        header[4] = 0x00 // revision
        header[5] = 0x00 // flags
        writeSynchSafeInt(header, 6, frameBytes.size)

        val out = ByteArrayOutputStream()
        out.write(header)
        out.write(frameBytes)
        return out.toByteArray()
    }

    private fun writeTextFrame(out: ByteArrayOutputStream, id: String, text: String) {
        val encoding = 0x03.toByte() // UTF-8
        // BOM not strictly needed for UTF-8 in ID3v2.3 but common practice
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + bom.size + textBytes.size).apply {
            this[0] = encoding
            System.arraycopy(bom, 0, this, 1, bom.size)
            System.arraycopy(textBytes, 0, this, 1 + bom.size, textBytes.size)
        }

        out.write(id.toByteArray(Charsets.US_ASCII))
        writeInt32BE(out, payload.size)
        out.write(byteArrayOf(0, 0)) // flags
        out.write(payload)
    }

    private fun writeApicFrame(out: ByteArrayOutputStream, cover: ByteArray) {
        val encoding = 0x00.toByte() // ISO-8859-1 for MIME in APIC is fine
        val mime = "image/jpeg".toByteArray(Charsets.US_ASCII)

        val payload = ByteArrayOutputStream()
        payload.write(encoding.toInt())
        payload.write(mime)
        payload.write(0) // null terminator for MIME
        payload.write(0x03) // picture type: front cover
        // Description (empty, null-terminated)
        payload.write(encoding.toInt())
        payload.write(0) // null terminator for empty description
        payload.write(cover)

        val payloadBytes = payload.toByteArray()
        out.write("APIC".toByteArray(Charsets.US_ASCII))
        writeInt32BE(out, payloadBytes.size)
        out.write(byteArrayOf(0, 0))
        out.write(payloadBytes)
    }

    private fun writeUsltFrame(out: ByteArrayOutputStream, lyric: String) {
        val encoding = 0x03.toByte() // UTF-8
        val language = "chi".toByteArray(Charsets.US_ASCII) // Chinese
        val description = byteArrayOf(0x00) // Empty description with null terminator

        val lyricBytes = lyric.toByteArray(Charsets.UTF_8)
        val payload = ByteArrayOutputStream()
        payload.write(encoding.toInt())
        payload.write(language)
        payload.write(description)
        payload.write(lyricBytes)

        val payloadBytes = payload.toByteArray()
        out.write("USLT".toByteArray(Charsets.US_ASCII))
        writeInt32BE(out, payloadBytes.size)
        out.write(byteArrayOf(0, 0)) // flags
        out.write(payloadBytes)
    }

    private fun writeInt32BE(out: ByteArrayOutputStream, value: Int) {
        out.write((value shr 24) and 0xff)
        out.write((value shr 16) and 0xff)
        out.write((value shr 8) and 0xff)
        out.write(value and 0xff)
    }

    private fun writeSynchSafeInt(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = ((value shr 21) and 0x7f).toByte()
        buf[offset + 1] = ((value shr 14) and 0x7f).toByte()
        buf[offset + 2] = ((value shr 7) and 0x7f).toByte()
        buf[offset + 3] = (value and 0x7f).toByte()
    }

    private fun readSynchSafeInt(buf: ByteArray, offset: Int): Int {
        return ((buf[offset].toInt() and 0x7f) shl 21) or
                ((buf[offset + 1].toInt() and 0x7f) shl 14) or
                ((buf[offset + 2].toInt() and 0x7f) shl 7) or
                (buf[offset + 3].toInt() and 0x7f)
    }

}
