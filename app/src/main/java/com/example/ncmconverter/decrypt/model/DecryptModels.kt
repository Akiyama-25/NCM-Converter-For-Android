package com.example.ncmconverter.decrypt.model

data class NcmMetadata(
    val musicId: Long = 0L,
    val musicName: String = "Unknown",
    val artists: List<String> = emptyList(),
    val album: String = "",
    val albumPic: String = "",
    val format: String = "",
    val duration: Long = 0L
)

data class DecryptResult(
    val audioData: ByteArray? = null,
    val tempAudioFile: java.io.File? = null,
    val metadata: NcmMetadata,
    val mimeType: String,
    val extension: String,
    val lyric: String? = null
) {
    val isFileBased: Boolean get() = tempAudioFile != null && audioData == null

    fun getAudioBytes(): ByteArray? = audioData
}

enum class DecryptState {
    IDLE,
    PARSING,
    DECRYPTING,
    SEARCHING_LYRIC,
    WRITING_META,
    COMPLETED,
    COMPLETED_NO_LYRIC,
    FAILED
}

data class DecryptProgress(
    val state: DecryptState = DecryptState.IDLE,
    val progress: Float = 0f,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val errorMessage: String? = null
)
