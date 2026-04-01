package com.rimaro.musify.data.extractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class InnerTubeSearch @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun search(title: String, artist: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val query = "$artist - $title"
                val body = buildRequestBody(query)

                val response = client.newCall(
                    okhttp3.Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.0 TV Safari/538.1")
                        .header("X-YouTube-Client-Name", "85")
                        .header("X-YouTube-Client-Version", "2.0")
                        .header("Origin", "https://www.youtube.com")
                        .header("Referer", "https://www.youtube.com")
                        .build()
                ).execute()

                if (!response.isSuccessful) {
                    Log.e("InnerTubeSearch", "Request failed: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                parseVideoId(responseBody)?.let { videoId ->
                    "https://www.youtube.com/watch?v=$videoId"
                }

            } catch (e: Exception) {
                Log.e("InnerTubeSearch", "Search failed", e)
                null
            }
        }
    }

    private fun buildRequestBody(videoId: String): RequestBody {
        val json = JSONObject().apply {
            put("videoId", videoId)
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "TVHTML5_SIMPLY_EMBEDDED_PLAYER")
                    put("clientVersion", "2.0")
                    put("hl", "en")
                    put("gl", "US")
                    put("utcOffsetMinutes", 0)
                })
                put("thirdParty", JSONObject().apply {
                    put("embedUrl", "https://www.youtube.com")
                })
            })
        }
        return json.toString().toRequestBody("application/json".toMediaTypeOrNull())    }

    private fun parseVideoId(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)

            val sectionContents = json
                .getJSONObject("contents")
                .getJSONObject("twoColumnSearchResultsRenderer")
                .getJSONObject("primaryContents")
                .getJSONObject("sectionListRenderer")
                .getJSONArray("contents")

            // iterate sections to find the first valid video
            for (i in 0 until sectionContents.length()) {
                val section = sectionContents.getJSONObject(i)
                if (!section.has("itemSectionRenderer")) continue

                val items = section
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents")

                for (j in 0 until items.length()) {
                    val item = items.getJSONObject(j)
                    if (!item.has("videoRenderer")) continue

                    val video = item.getJSONObject("videoRenderer")
                    val videoId = video.getString("videoId")

                    // skip live streams
                    val isLive = video
                        .optJSONObject("badges")
                        ?.toString()
                        ?.contains("LIVE", ignoreCase = true) == true

                    if (!isLive && videoId.isNotEmpty()) {
                        return videoId
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("InnerTubeSearch", "Parsing failed", e)
            null
        }
    }

    private suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val body = buildPlayerRequestBody(videoId)

                val response = client.newCall(
                    okhttp3.Request.Builder()
                        .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.0 TV Safari/538.1")
                        .header("X-YouTube-Client-Name", "85")
                        .header("X-YouTube-Client-Version", "2.0")
                        .header("Origin", "https://www.youtube.com")
                        .header("Referer", "https://www.youtube.com")
                        .build()

                ).execute()

                if (!response.isSuccessful) {
                    Log.e("InnerTubeSearch", "Player request failed: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                parseStreamUrl(responseBody)

            } catch (e: Exception) {
                Log.e("InnerTubeSearch", "Stream extraction failed", e)
                null
            }
        }
    }

    private fun buildPlayerRequestBody(videoId: String): RequestBody {
        val json = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", "ANDROID")
                    put("clientVersion", "19.09.37")
                    put("androidSdkVersion", 30)
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("videoId", videoId)
            put("playbackContext", JSONObject().apply {
                put("contentPlaybackContext", JSONObject().apply {
                    put("html5Preference", "HTML5_PREF_WANTS")
                })
            })
        }
        return json.toString().toRequestBody("application/json".toMediaTypeOrNull())
    }

    private fun parseStreamUrl(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)

            // check if video is playable
            val playabilityStatus = json
                .getJSONObject("playabilityStatus")
                .getString("status")

            if (playabilityStatus != "OK") {
                Log.e("InnerTubeSearch", "Video not playable: $playabilityStatus")
                return null
            }

            val streamingData = json.getJSONObject("streamingData")
            val adaptiveFormats = streamingData.getJSONArray("adaptiveFormats")

            // find best audio-only stream (m4a preferred)
            var bestM4a: Pair<Int, String>? = null   // bitrate to url
            var bestWebm: Pair<Int, String>? = null

            for (i in 0 until adaptiveFormats.length()) {
                val format = adaptiveFormats.getJSONObject(i)
                val mimeType = format.optString("mimeType", "")
                val bitrate = format.optInt("averageBitrate", 0)
                val url = format.optString("url", "")

                // skip video streams
                if (!mimeType.startsWith("audio/")) continue
                if (url.isEmpty()) continue

                when {
                    mimeType.contains("mp4") && (bestM4a == null || bitrate > bestM4a!!.first) ->
                        bestM4a = Pair(bitrate, url)
                    mimeType.contains("webm") && (bestWebm == null || bitrate > bestWebm!!.first) ->
                        bestWebm = Pair(bitrate, url)
                }
            }

            // prefer m4a over webm
            bestM4a?.second ?: bestWebm?.second

        } catch (e: Exception) {
            Log.e("InnerTubeSearch", "Stream URL parsing failed", e)
            null
        }
    }

    suspend fun extractDirect(sourceUrl: String): ExtractorResult.Success? {
        return try {
            val videoId = sourceUrl.toUri().getQueryParameter("v")
                ?: return null
            val streamUrl = getStreamUrl(videoId)
                ?: return null

            ExtractorResult.Success(
                streamUrl = streamUrl,
                expiresAt = System.currentTimeMillis() + 6 * 3600 * 1000L,
                sourceUrl = sourceUrl
            )
        } catch (e: Exception) {
            Log.e("NewPipeExtractorImpl", "InnerTube extractDirect also failed", e)
            null
        }
    }
}