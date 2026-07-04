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

    fun getService(baseUrl: String? = null): NcmApiService {
        val targetUrl = baseUrl?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("API 地址未设置，请在设置中配置")
        if (service == null || targetUrl != currentBaseUrl) {
            currentBaseUrl = targetUrl
            val base = if (targetUrl.endsWith("/")) targetUrl else "$targetUrl/"
            Log.d(TAG, "Creating Retrofit service with baseUrl: $base")
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
