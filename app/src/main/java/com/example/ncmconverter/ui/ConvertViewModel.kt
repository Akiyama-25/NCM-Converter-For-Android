package com.example.ncmconverter.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ncmconverter.api.RetrofitClient
import com.example.ncmconverter.decrypt.model.DecryptResult
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.decrypt.NcmDecryptor
import com.example.ncmconverter.lyric.LrcParser
import com.example.ncmconverter.lyric.LyricMatcher
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

data class FileItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Long,
    val state: DecryptState = DecryptState.IDLE,
    val progress: Float = 0f,
    val result: DecryptResult? = null,
    val error: String? = null
)

class ConvertViewModel(application: Application) : AndroidViewModel(application) {

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _completedItem = MutableSharedFlow<FileItem>(extraBufferCapacity = 10)
    val completedItem: SharedFlow<FileItem> = _completedItem.asSharedFlow()

    private var nextId = 0L

    companion object {
        private const val STREAMING_THRESHOLD = 30L * 1024 * 1024 // 30MB
    }

    fun addFiles(uris: List<Pair<Uri, String>>, getSize: (Uri) -> Long) {
        val newItems = uris.map { (uri, name) ->
            FileItem(
                id = nextId++,
                uri = uri,
                name = name,
                size = getSize(uri)
            )
        }
        _files.value = _files.value + newItems
    }

    fun removeFile(id: Long) {
        _files.value = _files.value.filter { it.id != id }
    }

    fun clearAll() {
        _files.value = emptyList()
    }

    /**
     * @param readBytes reads the entire URI content into a ByteArray (for small files)
     * @param openInputStream opens the URI as an InputStream (for large files, streaming)
     */
    fun decryptAll(
        readBytes: (Uri) -> ByteArray,
        openInputStream: (Uri) -> InputStream
    ) {
        val pending = _files.value.filter { it.state == DecryptState.IDLE || it.state == DecryptState.FAILED }
        if (pending.isEmpty()) return

        _isProcessing.value = true

        viewModelScope.launch {
            for (file in pending) {
                updateFileState(file.id, DecryptState.PARSING)

                try {
                    val decryptor = NcmDecryptor()
                    val result = if (file.size == 0L || file.size >= STREAMING_THRESHOLD) {
                        // Unknown size or large file: stream directly from URI InputStream
                        // Peak memory: ~128KB regardless of file size
                        val stream = withContext(Dispatchers.IO) { openInputStream(file.uri) }
                        stream.use { decryptor.decrypt(it, file.name) }
                    } else {
                        // Small file: in-memory decryption (original path)
                        val data = withContext(Dispatchers.IO) { readBytes(file.uri) }
                        decryptor.decrypt(data, file.name)
                    }

                    val finalResult = if (AppPrefs.enableLyric) {
                        updateFileState(file.id, DecryptState.SEARCHING_LYRIC)
                        fetchLyricForResult(result)
                    } else {
                        result
                    }

                    val state = if (finalResult.lyric != null) DecryptState.COMPLETED else DecryptState.COMPLETED_NO_LYRIC
                    updateFileState(file.id, state, result = finalResult)
                    _completedItem.tryEmit(
                        _files.value.find { it.id == file.id } ?: return@launch
                    )
                } catch (e: Exception) {
                    Log.e("NcmConverter", "Decrypt failed: ${file.name}", e)
                    updateFileState(file.id, DecryptState.FAILED, error = e.message ?: "未知错误")
                }
            }
        }
    }

    fun decryptSingle(
        id: Long,
        readBytes: (Uri) -> ByteArray,
        openInputStream: (Uri) -> InputStream
    ) {
        val file = _files.value.find { it.id == id } ?: return
        updateFileState(id, DecryptState.PARSING)

        viewModelScope.launch {
            try {
                val decryptor = NcmDecryptor()
                val result = if (file.size >= STREAMING_THRESHOLD) {
                    val stream = withContext(Dispatchers.IO) { openInputStream(file.uri) }
                    stream.use { decryptor.decrypt(it, file.name) }
                } else {
                    val data = withContext(Dispatchers.IO) { readBytes(file.uri) }
                    decryptor.decrypt(data, file.name)
                }

                val finalResult = if (AppPrefs.enableLyric) {
                    updateFileState(id, DecryptState.SEARCHING_LYRIC)
                    fetchLyricForResult(result)
                } else {
                    result
                }

                val state = if (finalResult.lyric != null) DecryptState.COMPLETED else DecryptState.COMPLETED_NO_LYRIC
                updateFileState(id, state, result = finalResult)
                _completedItem.tryEmit(
                    _files.value.find { it.id == id } ?: return@launch
                )
            } catch (e: Exception) {
                Log.e("NcmConverter", "Decrypt failed", e)
                updateFileState(id, DecryptState.FAILED, error = e.message ?: "未知错误")
            }
        }
    }

    private suspend fun fetchLyricForResult(result: DecryptResult): DecryptResult {
        return try {
            val baseUrl = AppPrefs.lyricApiBaseUrl
            val realIP = AppPrefs.lyricRealIP.takeIf { it.isNotBlank() }
            Log.d("LyricFlow", "Fetching lyric, baseUrl='$baseUrl', realIP=$realIP, musicId=${result.metadata.musicId}, name='${result.metadata.musicName}'")
            val api = RetrofitClient.getService(baseUrl)
            val matcher = LyricMatcher(api, realIP = realIP)
            val response = matcher.fetchLyric(result.metadata)
            if (response != null) {
                val original = LrcParser.parse(response.lrc?.lyric)
                val translated = LrcParser.parse(response.tlyric?.lyric)
                Log.d("LyricFlow", "Lyric found: original=${original.size} lines, translated=${translated.size} lines")
                val lyric = when (AppPrefs.lyricMode) {
                    "raw" -> LrcParser.toLrcString(original)
                    "translated" -> if (translated.isNotEmpty()) LrcParser.toLrcString(translated) else LrcParser.toLrcString(original)
                    else -> LrcParser.merge(original, translated)
                }
                if (lyric.isNotBlank()) result.copy(lyric = lyric) else result
            } else {
                Log.w("LyricFlow", "No lyric found for '${result.metadata.musicName}'")
                result
            }
        } catch (e: Exception) {
            Log.e("LyricFlow", "Failed to fetch lyric", e)
            result
        }
    }

    private fun updateFileState(
        id: Long,
        state: DecryptState,
        progress: Float = 0f,
        result: DecryptResult? = null,
        error: String? = null
    ) {
        _files.value = _files.value.map { file ->
            if (file.id == id) file.copy(state = state, progress = progress, result = result, error = error)
            else file
        }
        if (_files.value.all { it.state == DecryptState.COMPLETED || it.state == DecryptState.COMPLETED_NO_LYRIC || it.state == DecryptState.FAILED }) {
            _isProcessing.value = false
        }
    }
}
