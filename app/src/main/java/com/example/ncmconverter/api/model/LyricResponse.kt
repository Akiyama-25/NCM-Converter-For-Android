package com.example.ncmconverter.api.model

import com.google.gson.annotations.SerializedName

data class LyricResponse(
    @SerializedName("sgc") val sgc: Boolean?,           // has synced lyrics
    @SerializedName("sfy") val sfy: Boolean?,           // has translation
    @SerializedName("qfy") val qfy: Boolean?,           // has phonetic (romaji)
    @SerializedName("lrc") val lrc: LrcContent?,        // original lyrics
    @SerializedName("tlyric") val tlyric: LrcContent?,  // translated lyrics
    @SerializedName("klyric") val klyric: LrcContent?,  // karaoke (逐字) lyrics
    @SerializedName("romalrc") val romalrc: LrcContent?,// romanized lyrics
    @SerializedName("transUser") val transUser: TransUser?,
    @SerializedName("code") val code: Int
)

data class LrcContent(
    @SerializedName("version") val version: Int,
    @SerializedName("lyric") val lyric: String?
)

data class TransUser(
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("uptime") val uptime: Long?
)
