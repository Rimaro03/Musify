package com.rimaro.musify.data.local.extractor

import android.util.Log
import com.rimaro.musify.data.local.db.dao.TrackAudioUrlDao
import com.rimaro.musify.data.local.db.entity.TrackAudioUrl
import com.rimaro.musify.data.local.extractor.model.ExtractorResult
import com.rimaro.musify.domain.model.Track
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackUrlResolver @Inject constructor(
    private val dao: TrackAudioUrlDao,
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
            cached.expiresAt > System.currentTimeMillis() + 5 * 60 * 1000L
        ) {
            dao.updateLastPlayed(trackId.toString())
            Pair(cached.streamUrl, cached.sourceUrl)
        } else {
            try {
                val sourceUrl = cached?.sourceUrl
                if (sourceUrl != null) getFreshUrl(trackId.toString(), sourceUrl)
                else getFreshUrl(trackId.toString(), title, artist)

            } catch (e: ExtractionException) {
                Log.e("TrackUrlResolver", "Error resolving URL", e)
                Pair(null, null)
            }
        }

        return Pair(streamUrl, sourceUrl)
    }

    /**
     * Get a fresh audio stream url for a track by title and artist
     * @param trackId the id of the track
     * @param title the title of the track
     * @param artist the name of the artist
     * @return pair<streamUrl, sourceUrl>, both can be null if invalid
     * @throws ExtractionException if the extraction fails
     */
    suspend fun getFreshUrl(trackId: String, title: String, artist: String): Pair<String, String> {
        return when (val result = extractor.extract(title, artist)) {
            is ExtractorResult.Success -> {
                dao.upsert(
                    TrackAudioUrl(
                        id = trackId,
                        streamUrl = result.streamUrl,
                        sourceUrl = result.sourceUrl,
                        expiresAt = result.expiresAt,
                        lastPlayedAt = System.currentTimeMillis(),
                    )
                )
                Pair(result.streamUrl, result.sourceUrl)
            }
            is ExtractorResult.Failure -> throw ExtractionException(result.reason)
        }
    }

    /**
     * Get a fresh audio stream url for a track by source url
     * @param trackId the id of the track
     * @param sourceUrl the YouTube source url of the track
     * @return pair<streamUrl, sourceUrl>, both can be null if invalid
     * @throws ExtractionException if the extraction fails
     */
    suspend fun getFreshUrl(trackId: String, sourceUrl: String): Pair<String, String> {
        return when (val result = extractor.extractDirect(sourceUrl)) {
            is ExtractorResult.Success -> {
                dao.upsert(
                    TrackAudioUrl(
                        id = trackId,
                        streamUrl = result.streamUrl,
                        sourceUrl = result.sourceUrl,
                        expiresAt = result.expiresAt,
                        lastPlayedAt = System.currentTimeMillis(),
                    )
                )
                Pair(result.streamUrl, result.sourceUrl)
            }
            is ExtractorResult.Failure -> throw ExtractionException(result.reason)
        }
    }
}