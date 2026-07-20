package com.example.ncmconverter.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ncmconverter.api.RetrofitClient
import com.example.ncmconverter.decrypt.NcmDecryptor
import com.example.ncmconverter.decrypt.model.DecryptResult
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.lyric.LrcParser
import com.example.ncmconverter.lyric.LyricMatcher
import com.example.ncmconverter.service.DecryptService
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

    companion object {
        var instance: ConvertViewModel? = null
            private set
        private const val STREAMING_THRESHOLD = 30L * 1024 * 1024
        private const val TAG = "ConvertViewModel"
    }

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _completedItem = MutableSharedFlow<FileItem>(extraBufferCapacity = 10)
    val completedItem: SharedFlow<FileItem> = _completedItem.asSharedFlow()

    private var nextId = 0L
    internal var pendingSingleId: Long? = null

    init {
        instance = this
    }

    override fun onCleared() {
        super.onCleared()
        if (instance === this) instance = null
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

    fun decryptAll() {
        _isProcessing.value = true
        if (AppPrefs.autoSave) {
            // Background conversion via foreground service
            val service = DecryptService.instance
            if (service != null) {
                service.startConversion()
            } else {
                getApplication<Application>().startForegroundService(
                    Intent(getApplication(), DecryptService::class.java)
                )
            }
        } else {
            // Direct conversion in ViewModel scope
            val pending = _files.value.filter {
                it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
            }
            if (pending.isEmpty()) { _isProcessing.value = false; return }
            for (file in pending) {
                viewModelScope.launch { convertDirect(file) }
            }
        }
    }

    fun decryptSingle(id: Long) {
        _isProcessing.value = true
        if (AppPrefs.autoSave) {
            val service = DecryptService.instance
            if (service != null) {
                service.startConversionSingle(id)
            } else {
                pendingSingleId = id
                getApplication<Application>().startForegroundService(
                    Intent(getApplication(), DecryptService::class.java)
                )
            }
        } else {
            val file = _files.value.find { it.id == id } ?: return
            viewModelScope.launch { convertDirect(file) }
        }
    }

    private suspend fun convertDirect(file: FileItem) {
        val cr = getApplication<Application>().contentResolver
        updateFileState(file.id, DecryptState.PARSING)

        try {
            val decryptor = NcmDecryptor()
            val result = if (file.size == 0L || file.size >= STREAMING_THRESHOLD) {
                val stream = withContext(Dispatchers.IO) {
                    cr.openInputStream(file.uri) ?: throw IllegalArgumentException("Cannot open file")
                }
                stream.use { decryptor.decrypt(it, file.name) }
            } else {
                val data = withContext(Dispatchers.IO) {
                    cr.openInputStream(file.uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Cannot open file")
                }
                decryptor.decrypt(data, file.name)
            }

            val finalResult = if (AppPrefs.enableLyric) {
                updateFileState(file.id, DecryptState.SEARCHING_LYRIC)
                fetchLyric(result)
            } else {
                result
            }

            val state = if (finalResult.lyric != null) DecryptState.COMPLETED
                        else DecryptState.COMPLETED_NO_LYRIC
            updateFileState(file.id, state, result = finalResult)
            _completedItem.tryEmit(
                _files.value.find { it.id == file.id } ?: return
            )
        } catch (e: Exception) {
            Log.e(TAG, "Convert failed: ${file.name}", e)
            updateFileState(file.id, DecryptState.FAILED, error = e.message ?: "Unknown error")
        }
    }

    private suspend fun fetchLyric(result: DecryptResult): DecryptResult {
        return try {
            val baseUrl = AppPrefs.lyricApiBaseUrl
            val realIP = AppPrefs.lyricRealIP.takeIf { it.isNotBlank() }
            val api = RetrofitClient.getService(baseUrl)
            val matcher = LyricMatcher(api, realIP = realIP)
            val response = matcher.fetchLyric(result.metadata)
            if (response != null) {
                val original = LrcParser.parse(response.lrc?.lyric)
                val translated = LrcParser.parse(response.tlyric?.lyric)
                val lyric = when (AppPrefs.lyricMode) {
                    "raw" -> LrcParser.toLrcString(original)
                    "translated" -> if (translated.isNotEmpty()) LrcParser.toLrcString(translated) else LrcParser.toLrcString(original)
                    else -> LrcParser.merge(original, translated)
                }
                if (lyric.isNotBlank()) result.copy(lyric = lyric) else result
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lyric", e)
            result
        }
    }

    fun emitCompletedItem(item: FileItem) {
        _completedItem.tryEmit(item)
    }

    fun clearResult(id: Long) {
        _files.value = _files.value.map { file ->
            if (file.id == id) file.copy(result = null) else file
        }
    }

    internal fun updateFileState(
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
        if (_files.value.all {
            it.state == DecryptState.COMPLETED ||
            it.state == DecryptState.COMPLETED_NO_LYRIC ||
            it.state == DecryptState.FAILED
        }) {
            _isProcessing.value = false
        }
    }
}
