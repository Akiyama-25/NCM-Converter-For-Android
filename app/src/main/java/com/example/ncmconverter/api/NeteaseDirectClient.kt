package com.example.ncmconverter.api

import android.util.Log
import com.example.ncmconverter.api.model.LrcContent
import com.example.ncmconverter.api.model.LyricResponse
import com.example.ncmconverter.api.model.SearchResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Direct HTTP transport client connecting directly to NetEase Cloud Music official endpoints.
 * Operates without requiring an external cloud server / NeteaseCloudMusicApiEnhanced deployment.
 */
class NeteaseDirectClient(
    private val client: OkHttpClient = defaultClient
) {
    companion object {
        private const val TAG = "NeteaseDirectClient"
        private const val UA_ANDROID =
            "NeteaseMusic/9.1.65 (Linux; U; Android 14; zh_CN; Pixel 8; Build/UD1A.230803.041)"
        private const val BASE_COOKIE =
            "os=android; appver=9.1.65; channel=netease; osver=14;"

        private val CHINA_IPS = listOf(
            "116.25.146.177",
            "183.14.132.221",
            "220.181.38.148",
            "120.232.145.185",
            "117.136.8.12",
            "223.73.1.20",
            "58.216.2.14"
        )

        val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }

        private val gson = Gson()
    }

    private fun getFakeIp(realIp: String?): String {
        return realIp?.takeIf { it.isNotBlank() } ?: CHINA_IPS.random()
    }

    suspend fun postEapi(
        url: String,
        apiPath: String,
        jsonPayload: String,
        realIp: String? = null
    ): String = withContext(Dispatchers.IO) {
        val encrypted = NeteaseCrypto.eapi(apiPath, jsonPayload)
        val ip = getFakeIp(realIp)

        val formBody = FormBody.Builder()
            .add("params", encrypted)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .header("User-Agent", UA_ANDROID)
            .header("Cookie", BASE_COOKIE)
            .header("X-Real-IP", ip)
            .header("X-Forwarded-For", ip)
            .header("Referer", NeteaseEndpoints.HOST_DEFAULT)
            .build()

        val response = client.newCall(request).execute()
        val rawBody = response.body?.string()?.trim() ?: ""

        if (rawBody.length >= 32 && rawBody.matches(Regex("^[0-9a-fA-F]+$"))) {
            NeteaseCrypto.decryptEapiResponse(rawBody) ?: rawBody
        } else {
            rawBody
        }
    }

    suspend fun search(
        keywords: String,
        type: Int = 1,
        limit: Int = 15,
        offset: Int = 0,
        realIP: String? = null
    ): SearchResponse = withContext(Dispatchers.IO) {
        if (keywords.isBlank()) {
            return@withContext SearchResponse(result = null, code = 200)
        }
        val header = "{\"os\":\"android\",\"appver\":\"9.1.65\",\"requestId\":\"${System.currentTimeMillis()}_0001\"}"
        val payload = buildString {
            append("{")
            append("\"s\":").append(gson.toJson(keywords.trim())).append(",")
            append("\"type\":").append(type).append(",")
            append("\"limit\":").append(limit).append(",")
            append("\"offset\":").append(offset).append(",")
            append("\"total\":true,")
            append("\"header\":").append(header)
            append("}")
        }
        try {
            val body = postEapi(
                url = NeteaseEndpoints.URL_EAPI_CLOUDSEARCH,
                apiPath = NeteaseEndpoints.PATH_EAPI_CLOUDSEARCH,
                jsonPayload = payload,
                realIp = realIP
            )
            gson.fromJson(body, SearchResponse::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Direct search failed: ${e.message}")
            SearchResponse(result = null, code = 500)
        }
    }

    suspend fun getLyric(
        id: Long,
        lv: Int = -1,
        kv: Int = -1,
        tv: Int = -1,
        realIP: String? = null
    ): LyricResponse = withContext(Dispatchers.IO) {
        val payload = buildString {
            append("{")
            append("\"id\":").append(id).append(",")
            append("\"lv\":").append(lv).append(",")
            append("\"tv\":").append(tv).append(",")
            append("\"kv\":").append(kv)
            append("}")
        }
        try {
            val body = postEapi(
                url = NeteaseEndpoints.URL_EAPI_SONG_LYRIC,
                apiPath = NeteaseEndpoints.PATH_EAPI_SONG_LYRIC,
                jsonPayload = payload,
                realIp = realIP
            )
            val response = gson.fromJson(body, LyricResponse::class.java)
            normalizeLyricResponse(response)
        } catch (e: Exception) {
            Log.w(TAG, "Direct getLyric failed: ${e.message}")
            LyricResponse(
                sgc = null,
                sfy = null,
                qfy = null,
                lrc = null,
                tlyric = null,
                klyric = null,
                romalrc = null,
                transUser = null,
                code = 500
            )
        }
    }

    /**
     * If lyric text is returned in YRC JSON format, convert it to standard [mm:ss.xxx] LRC lines.
     */
    private fun normalizeLyricResponse(response: LyricResponse?): LyricResponse {
        if (response == null) {
            return LyricResponse(
                sgc = null, sfy = null, qfy = null, lrc = null,
                tlyric = null, klyric = null, romalrc = null, transUser = null, code = 500
            )
        }
        val normLrc = response.lrc?.let {
            val converted = convertYrcJsonToLrc(it.lyric)
            if (converted != it.lyric) LrcContent(it.version, converted) else it
        }
        val normTLrc = response.tlyric?.let {
            val converted = convertYrcJsonToLrc(it.lyric)
            if (converted != it.lyric) LrcContent(it.version, converted) else it
        }
        return response.copy(lrc = normLrc, tlyric = normTLrc)
    }

    private fun convertYrcJsonToLrc(jsonLrc: String?): String? {
        if (jsonLrc == null || !jsonLrc.trim().startsWith("{")) return jsonLrc
        val sb = StringBuilder()
        val linePattern = Pattern.compile("\\{\"t\":\\s*(\\d+)[^}]*?\"c\":\\s*\\[(.*?)\\]\\}")
        val txPattern = Pattern.compile("\"tx\":\\s*\"(.*?)\"")
        val m = linePattern.matcher(jsonLrc)
        while (m.find()) {
            val t = m.group(1)?.toLongOrNull() ?: continue
            val chunks = m.group(2) ?: ""
            val lineText = StringBuilder()
            val txMatcher = txPattern.matcher(chunks)
            while (txMatcher.find()) {
                val tx = txMatcher.group(1)
                    ?.replace("\\n", "")
                    ?.replace("\\r", "")
                    ?.replace("\\\"", "\"") ?: ""
                lineText.append(tx)
            }
            val minutes = t / 60000
            val seconds = (t % 60000) / 1000
            val millis = t % 1000
            sb.append(String.format("[%02d:%02d.%03d]%s\n", minutes, seconds, millis, lineText.toString().trim()))
        }
        val res = sb.toString().trim()
        return if (res.isNotEmpty()) res else jsonLrc
    }
}