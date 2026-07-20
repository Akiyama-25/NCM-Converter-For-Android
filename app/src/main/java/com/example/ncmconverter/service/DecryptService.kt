package com.example.ncmconverter.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ncmconverter.MainActivity
import com.example.ncmconverter.R
import com.example.ncmconverter.api.RetrofitClient
import com.example.ncmconverter.decrypt.NcmDecryptor
import com.example.ncmconverter.decrypt.model.DecryptResult
import com.example.ncmconverter.decrypt.model.DecryptState
import com.example.ncmconverter.lyric.LrcParser
import com.example.ncmconverter.lyric.LyricMatcher
import com.example.ncmconverter.ui.ConvertViewModel
import com.example.ncmconverter.ui.FileItem
import com.example.ncmconverter.util.AppPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DecryptService : Service() {

    companion object {
        private const val CHANNEL_ID = "decrypt_progress"
        private const val NOTIFICATION_ID = 1001
        private const val STREAMING_THRESHOLD = 30L * 1024 * 1024
        private const val TAG = "DecryptService"

        var instance: DecryptService? = null
            private set

        private val _progress = MutableStateFlow(0 to 0)
        val progress: StateFlow<Pair<Int, Int>> = _progress.asStateFlow()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val viewModel get() = ConvertViewModel.instance

    @Volatile
    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_text)))

        // Check if ViewModel has a pending conversion (race condition fix)
        val vm = viewModel
        if (vm != null && vm.isProcessing.value) {
            val pendingId = vm.pendingSingleId
            if (pendingId != null) {
                vm.pendingSingleId = null
                startConversionSingle(pendingId)
            } else {
                startConversion()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun startConversion() {
        if (isProcessing) return
        val vm = viewModel ?: return
        val pending = vm.files.value.filter {
            it.state == DecryptState.IDLE || it.state == DecryptState.FAILED
        }
        if (pending.isEmpty()) return

        isProcessing = true
        _progress.value = 0 to pending.size
        updateNotification(getString(R.string.notif_progress, 0, pending.size))

        scope.launch {
            try {
                var completed = 0
                for (file in pending) {
                    convertFile(file)
                    completed++
                    _progress.value = completed to pending.size
                    updateNotification(getString(R.string.notif_progress, completed, pending.size))
                }
            } finally {
                isProcessing = false
                stopSelf()
            }
        }
    }

    fun startConversionSingle(id: Long) {
        if (isProcessing) return
        val vm = viewModel ?: return
        val file = vm.files.value.find { it.id == id } ?: return

        isProcessing = true
        _progress.value = 0 to 1
        updateNotification(getString(R.string.notif_text))

        scope.launch {
            try {
                convertFile(file)
                _progress.value = 1 to 1
            } finally {
                isProcessing = false
                stopSelf()
            }
        }
    }

    private suspend fun convertFile(file: FileItem) {
        val vm = viewModel ?: return
        vm.updateFileState(file.id, DecryptState.PARSING)

        try {
            val decryptor = NcmDecryptor()
            val result = if (file.size == 0L || file.size >= STREAMING_THRESHOLD) {
                val stream = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(file.uri)
                        ?: throw IllegalArgumentException("Cannot open file")
                }
                stream.use { decryptor.decrypt(it, file.name) }
            } else {
                val data = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(file.uri)?.use { it.readBytes() }
                        ?: throw IllegalArgumentException("Cannot open file")
                }
                decryptor.decrypt(data, file.name)
            }

            val finalResult = if (AppPrefs.enableLyric) {
                vm.updateFileState(file.id, DecryptState.SEARCHING_LYRIC)
                fetchLyricForResult(result)
            } else {
                result
            }

            val state = if (finalResult.lyric != null) DecryptState.COMPLETED
                        else DecryptState.COMPLETED_NO_LYRIC
            vm.updateFileState(file.id, state, result = finalResult)
            vm.emitCompletedItem(
                vm.files.value.find { it.id == file.id } ?: return
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Convert failed: ${file.name}", e)
            vm.updateFileState(file.id, DecryptState.FAILED, error = e.message ?: "Unknown error")
        }
    }

    private suspend fun fetchLyricForResult(result: DecryptResult): DecryptResult {
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

    private fun buildNotification(text: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
