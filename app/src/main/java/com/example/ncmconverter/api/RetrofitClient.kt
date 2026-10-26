package com.example.ncmconverter.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val TAG = "NcmApi"

    private var currentBaseUrl: String = ""
    private var service: NcmApiService? = null
    private val directService: NcmApiService by lazy { DirectNcmApiService() }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Referer", "https://music.163.com/")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build()
                Log.d(TAG, "Request: ${request.method} ${request.url}")
                val response = chain.proceed(request)
                Log.d(TAG, "Response: ${response.code} ${request.url}")
                response
            }
            .addInterceptor(logging)
            .followRedirects(true)
            .build()
    }

    /**
     * Get [NcmApiService] instance.
     * When [baseUrl] is null, blank, or points to music.163.com, returns [DirectNcmApiService]
     * which talks directly to NetEase official servers via encrypted EAPI signatures on the client.
     * When [baseUrl] is specified with a custom proxy/server address, returns a Retrofit-backed service.
     */
    fun getService(baseUrl: String? = null): NcmApiService {
        val targetUrl = baseUrl?.trim() ?: ""
        if (targetUrl.isBlank() || targetUrl.contains("music.163.com")) {
            return directService
        }

        if (service == null || targetUrl != currentBaseUrl) {
            currentBaseUrl = targetUrl
            val base = if (targetUrl.endsWith("/")) targetUrl else "$targetUrl/"
            Log.d(TAG, "Creating Retrofit service with custom baseUrl: $base")
            val retrofit = Retrofit.Builder()
                .baseUrl(base)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            service = retrofit.create(NcmApiService::class.java)
        }
        return service!!
    }
}