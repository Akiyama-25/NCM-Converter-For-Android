package com.example.ncmconverter.api

/**
 * NetEase Cloud Music official endpoints and host routing constants.
 */
object NeteaseEndpoints {
    const val HOST_DEFAULT = "https://music.163.com"
    const val HOST_SERVICE = "https://interface.music.163.com"

    // EAPI endpoints (actual URL paths start with /eapi, plain crypto paths start with /api)
    const val PATH_EAPI_CLOUDSEARCH = "/api/cloudsearch/pc"
    const val URL_EAPI_CLOUDSEARCH = "$HOST_SERVICE/eapi/cloudsearch/pc"

    const val PATH_EAPI_SONG_LYRIC = "/api/song/lyric"
    const val URL_EAPI_SONG_LYRIC = "$HOST_SERVICE/eapi/song/lyric"
}