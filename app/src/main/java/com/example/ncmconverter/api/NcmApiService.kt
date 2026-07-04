package com.example.ncmconverter.api

import com.example.ncmconverter.api.model.LyricResponse
import com.example.ncmconverter.api.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NcmApiService {

    @GET("cloudsearch")
    suspend fun search(
        @Query("keywords") keywords: String,
        @Query("type") type: Int = 1,
        @Query("limit") limit: Int = 15,
        @Query("offset") offset: Int = 0,
        @Query("realIP") realIP: String? = null
    ): SearchResponse

    @GET("lyric")
    suspend fun getLyric(
        @Query("id") id: Long,
        @Query("lv") lv: Int = -1,
        @Query("kv") kv: Int = -1,
        @Query("tv") tv: Int = -1,
        @Query("realIP") realIP: String? = null
    ): LyricResponse
}
