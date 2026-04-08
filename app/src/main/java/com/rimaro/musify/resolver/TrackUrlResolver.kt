package com.rimaro.musify.resolver

import android.util.Log
import com.rimaro.musify.data.extractor.ExtractorResult
import com.rimaro.musify.data.extractor.TrackExtractor
import com.rimaro.musify.data.local.db.CachedTrack
import com.rimaro.musify.data.local.db.TrackDao
import com.rimaro.musify.data.local.db.TrackStatus
import com.rimaro.musify.domain.model.Track
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackUrlResolver @Inject constructor(
    private val dao: TrackDao,
    private val extractor: TrackExtractor
) {
    suspend fun resolve(track: Track): Track? {
        val trackId = track.id
        val title = track.title
        val artist = track.artist

        val cached = dao.getTrack(trackId.toString())

        // Return cached URL if still valid
        val streamUrl = if (cached != null &&
            cached.status == TrackStatus.ACTIVE &&
            cached.expiresAt > System.currentTimeMillis() + 5 * 60 * 1000L &&
            cached.failureCount < 5
        ) {
            dao.updateLastPlayed(trackId.toString())
            cached.streamUrl
        } else {
            try {
                getFreshUrl(trackId.toString(), title, artist, cached)
            } catch (e: ExtractionException) {
                Log.e("TrackUrlResolver", "Error resolving URL", e)
                return null
            }
        }

        val fetchedTrack = track.copy(streamUrl = streamUrl)
        return fetchedTrack
    }

    suspend fun getFreshUrl(trackId: String, title: String, artist: String, cached: CachedTrack? = null): String {
        // Try to extract a fresh URL
        val sourceUrl = cached?.sourceUrl

        val result = if (sourceUrl != null) {
            extractor.extractDirect(sourceUrl)
        } else {
            extractor.extract(title, artist)
        }

        return when (result) {
            is ExtractorResult.Success -> {
                dao.upsert(
                    CachedTrack(
                        id = trackId,
                        title = title,
                        artist = artist,
                        streamUrl = result.streamUrl,
                        sourceUrl = result.sourceUrl,
                        expiresAt = result.expiresAt,
                        lastPlayedAt = System.currentTimeMillis(),
                        failureCount = 0,
                        status = TrackStatus.ACTIVE
                    )
                )
                result.streamUrl
            }
            is ExtractorResult.Failure -> {
                val newCount = (cached?.failureCount ?: 0) + 1
                val backoffMs = when (newCount) {
                    1 -> 1 * 3600 * 1000L
                    2 -> 4 * 3600 * 1000L
                    3 -> 24 * 3600 * 1000L
                    else -> Long.MAX_VALUE
                }
                dao.updateFailure(
                    trackId = trackId,
                    count = newCount,
                    nextRetryAt = System.currentTimeMillis() + backoffMs
                )
                if (newCount >= 5) dao.updateStatus(trackId, TrackStatus.UNRESOLVABLE)
                throw ExtractionException(result.reason)
            }
        }
    }
}