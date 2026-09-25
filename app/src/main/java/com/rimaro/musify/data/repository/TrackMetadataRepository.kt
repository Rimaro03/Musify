package com.rimaro.musify.data.repository

import com.rimaro.musify.data.local.db.dao.TrackMetadataDao
import com.rimaro.musify.data.local.db.entity.toTrack
import com.rimaro.musify.data.remote.deezer.dto.toTrackMetadata
import com.rimaro.musify.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackMetadataRepository @Inject constructor (
    private val trackMetadataDao: TrackMetadataDao,
    private val deezerRepository: DeezerRepository
) {
    /**
     * Get track metadata from cache. If a track id is not cached, it's fetched and inserted first
     * @param trackIds list of the track ids
     * @return Result.success with the list of fetched tracks, or Result.failure with the exception if any error occur
     */
    suspend fun getTracks(trackIds: List<Long>): Result<List<Track>> {
        val tracksMetadata = trackMetadataDao.getByIds(trackIds.map{ it.toString() })
        val trackMetadataIds = tracksMetadata.map { it.trackId }.toSet()
        val missingIds = trackIds.filter { id ->
            id !in trackMetadataIds
        }

        if(missingIds.isEmpty()) return Result.success(tracksMetadata.map { it.toTrack() })

        return try {
            val newTracks = deezerRepository.getTrackByIds(missingIds).map { it.toTrackMetadata() }
            trackMetadataDao.upsertAll(newTracks)
            val newIds = newTracks.map { it.trackId }.toSet()
            val cachedTracks = tracksMetadata.filter { it.trackId !in newIds }
            Result.success((newTracks + cachedTracks).map { it.toTrack() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}