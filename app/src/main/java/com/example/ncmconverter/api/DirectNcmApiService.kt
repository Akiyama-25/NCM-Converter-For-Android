package com.example.ncmconverter.api

import com.example.ncmconverter.api.model.LyricResponse
import com.example.ncmconverter.api.model.SearchResponse

/**
 * Implementation of [NcmApiService] using [NeteaseDirectClient] that queries NetEase official servers directly.
 */
class DirectNcmApiService(
    private val directClient: NeteaseDirectClient = NeteaseDirectClient()
) : NcmApiService {

    override suspend fun search(
        keywords: String,
        type: Int,
        limit: Int,
        offset: Int,
        realIP: String?
    ): SearchResponse {
        return directClient.search(
            keywords = keywords,
            type = type,
            limit = limit,
            offset = offset,
            realIP = realIP
        )
    }

    override suspend fun getLyric(
        id: Long,
        lv: Int,
        kv: Int,
        tv: Int,
        realIP: String?
    ): LyricResponse {
        return directClient.getLyric(
            id = id,
            lv = lv,
            kv = kv,
            tv = tv,
            realIP = realIP
        )
    }
}