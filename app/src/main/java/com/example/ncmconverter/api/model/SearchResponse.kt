package com.example.ncmconverter.api.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("result") val result: SearchResult?,
    @SerializedName("code") val code: Int
)

data class SearchResult(
    @SerializedName("songs") val songs: List<SongInfo>?,
    @SerializedName("songCount") val songCount: Int,
    @SerializedName("hasMore") val hasMore: Boolean
)

data class SongInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("ar") val artists: List<ArtistInfo>?,      // artists array
    @SerializedName("al") val album: AlbumInfo?,                // album object
    @SerializedName("dt") val duration: Long = 0,               // duration in ms
    @SerializedName("alias") val alias: List<String>?,
    @SerializedName("mv") val mvId: Long = 0,                   // MV id
    @SerializedName("fee") val fee: Int = 0                     // 0=free, 1=VIP, 4=paid album, 8=free+low quality
)

data class ArtistInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

data class AlbumInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("picUrl") val picUrl: String?
)
