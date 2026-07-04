package com.example.ncmconverter.lyric

import android.util.Log
import com.example.ncmconverter.api.NcmApiService
import com.example.ncmconverter.api.model.LyricResponse
import com.example.ncmconverter.api.model.SongInfo
import com.example.ncmconverter.decrypt.model.NcmMetadata

class LyricMatcher(
    private val api: NcmApiService,
    private val minimumSimilarity: Double = 0.65,
    private val realIP: String? = null
) {

    suspend fun fetchLyric(metadata: NcmMetadata): LyricResponse? {
        // Strategy 1: Direct lookup by musicId from NCM file
        if (metadata.musicId > 0) {
            try {
                Log.d("LyricMatcher", "Strategy 1: Direct lookup by musicId=${metadata.musicId}")
                val response = api.getLyric(metadata.musicId, realIP = realIP)
                Log.d("LyricMatcher", "getLyric response: code=${response.code}, hasLrc=${!response.lrc?.lyric.isNullOrBlank()}")
                if (response.code == 200 && !response.lrc?.lyric.isNullOrBlank()) {
                    return response
                }
            } catch (e: Exception) {
                Log.w("LyricMatcher", "Strategy 1 failed", e)
            }
        }

        // Strategy 2: Search by song name + artist
        Log.d("LyricMatcher", "Strategy 2: Search by name+artist")
        return searchAndMatch(metadata)
    }

    private suspend fun searchAndMatch(metadata: NcmMetadata): LyricResponse? {
        val songName = metadata.musicName
        if (songName.isBlank() || songName == "Unknown") {
            Log.w("LyricMatcher", "songName is blank or Unknown, skip search")
            return null
        }

        // Build search keywords: "songName artist1 artist2"
        val keywords = if (metadata.artists.isNotEmpty()) {
            "$songName ${metadata.artists.joinToString(" ")}"
        } else {
            songName
        }

        Log.d("LyricMatcher", "Searching with keywords: '$keywords'")

        // Try searching with full metadata
        val result = trySearch(keywords, metadata)
            // Fallback: search with song name only
            ?: if (metadata.artists.isNotEmpty()) {
                Log.d("LyricMatcher", "Fallback: search with songName only")
                trySearch(songName, metadata)
            } else null
            // Fallback: strip extra info from song name (brackets, Cover, feat.)
            ?: run {
                val stripped = SimilarityCalculator.stripExtraInfo(songName)
                Log.d("LyricMatcher", "Fallback: search with stripped name: '$stripped'")
                trySearch(stripped, metadata)
            }

        return result
    }

    private suspend fun trySearch(keywords: String, metadata: NcmMetadata): LyricResponse? {
        try {
            val response = api.search(keywords, type = 1, limit = 15, realIP = realIP)
            Log.d("LyricMatcher", "search response: code=${response.code}, songCount=${response.result?.songCount}")
            if (response.code != 200) return null

            val songs = response.result?.songs
            if (songs.isNullOrEmpty()) {
                Log.d("LyricMatcher", "No songs found")
                return null
            }
            Log.d("LyricMatcher", "Found ${songs.size} songs, first: '${songs[0].name}' by ${songs[0].artists?.joinToString { it.name }}")

            val bestMatch = findBestMatch(songs, metadata)
            if (bestMatch == null) {
                Log.d("LyricMatcher", "No match above threshold $minimumSimilarity")
                return null
            }
            Log.d("LyricMatcher", "Best match: '${bestMatch.name}' id=${bestMatch.id}")

            // Fetch lyric for the matched song
            val lyricResponse = api.getLyric(bestMatch.id, realIP = realIP)
            Log.d("LyricMatcher", "getLyric response: code=${lyricResponse.code}, hasLrc=${!lyricResponse.lrc?.lyric.isNullOrBlank()}")
            if (lyricResponse.code == 200 && !lyricResponse.lrc?.lyric.isNullOrBlank()) {
                return lyricResponse
            }
        } catch (e: Exception) {
            Log.e("LyricMatcher", "trySearch failed for '$keywords'", e)
        }
        return null
    }

    private fun findBestMatch(songs: List<SongInfo>, metadata: NcmMetadata): SongInfo? {
        val targetArtists = metadata.artists

        val scored = songs.mapNotNull { song ->
            val resultArtists = song.artists?.map { it.name } ?: emptyList()
            val score = SimilarityCalculator.calculateMatchScore(
                targetName = metadata.musicName,
                targetArtists = targetArtists,
                resultName = song.name,
                resultArtists = resultArtists
            )
            Log.d("LyricMatcher", "Score: ${"%.2f".format(score)} for '${song.name}' by ${resultArtists.joinToString()}")
            if (score >= minimumSimilarity) song to score else null
        }

        return scored.maxByOrNull { it.second }?.first
    }
}
