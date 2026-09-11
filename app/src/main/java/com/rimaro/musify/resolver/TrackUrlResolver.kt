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
    /**
     * Resolve the audio url of the provided track
     * @param track instance of class Track to resolve
     * @return pair<streamUrl, sourceUrl>, both can be null if invalid
     * */
    suspend fun resolve(track: Track): Pair<String?, String?> {
        val trackId = track.id
        val title = track.title
        val artist = track.artist

        val cached = dao.getTrack(trackId.toString())

        // Return cached URL if still valid
        val (streamUrl, sourceUrl) = if (cached != null &&
            cached.status == TrackStatus.ACTIVE &&
            cached.expiresAt > System.currentTimeMillis() + 5 * 60 * 1000L &&
            cached.failureCount < 5
        ) {
            dao.updateLastPlayed(trackId.toString())
            Pair(cached.streamUrl, cached.sourceUrl)
        } else {
            try {
                getFreshUrl(trackId.toString(), title, artist, cached)
            } catch (e: ExtractionException) {
                Log.e("TrackUrlResolver", "Error resolving URL", e)
                Pair(null, null)
            }
        }

        return Pair(streamUrl, sourceUrl)
    }

    /**
     * Get a fresh audio stream url for a track if the cached one is invalid/expired
     * @param trackId the id of the track
     * @param title the title of the track
     * @param artist the name of the artist
     * @param cached instance of CachedTrack, containing the streamUrl
     * @return pair<streamUrl, sourceUrl>, both can be null if invalid
     * @throws ExtractionException if the extraction fails
     */
    suspend fun getFreshUrl(trackId: String, title: String, artist: String, cached: CachedTrack? = null): Pair<String, String> {
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
                Pair(result.streamUrl, result.sourceUrl)
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