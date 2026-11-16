package com.example.ncmconverter.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object CoverFetcher {

    suspend fun fetch(url: String): ByteArray? {
        if (url.isBlank()) {
            Log.d("CoverArt", "albumPic is blank, skip")
            return null
        }
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val safeUrl = url.trim().replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
                if (!safeUrl.startsWith("https://", ignoreCase = true)) {
                    Log.w("CoverArt", "Skip non-HTTPS cover URL")
                    return@withContext null
                }
                Log.d("CoverArt", "Fetching cover: $safeUrl")
                connection = (URL(safeUrl).openConnection() as? HttpURLConnection) ?: return@withContext null
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.connect()
                Log.d("CoverArt", "Response: ${connection.responseCode}")
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val bytes = connection.inputStream.use { it.readBytes() }
                    Log.d("CoverArt", "Cover downloaded: ${bytes.size} bytes")
                    bytes
                } else null
            } catch (e: Exception) {
                Log.e("CoverArt", "Failed to fetch cover", e)
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
