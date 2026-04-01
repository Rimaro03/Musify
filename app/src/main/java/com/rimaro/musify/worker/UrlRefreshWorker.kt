package com.rimaro.musify.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rimaro.musify.data.local.db.TrackDao
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UrlRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trackDao: TrackDao,
    private val trackUrlResolver: TrackUrlResolver
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        /*
        * 0. Mark old tracks as COLD
        * 1. Get tracks expiring in time range
        * 2. For each track, refresh the URL
        * 3. Update the database with the new URL
        * */

        try {
            trackDao.markColdTracks(
                coldThreshold = System.currentTimeMillis() - COLD_TRACKS_INTERVAL
            )

            val expiringTracks = trackDao.getTracksToRefresh(
                threshold = System.currentTimeMillis() + REFRESH_INTERVAL,
            )
            expiringTracks.forEach { track ->
                val newUrl = trackUrlResolver.getFreshUrl(track.id, track.title, track.artist)
                trackDao.upsert(track.copy(streamUrl = newUrl))
            }
        } catch (e: Exception) {
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val COLD_TRACKS_INTERVAL: Long = 28 * 24 * 3600 * 1000L // 4 weeks before track goes cold
        const val REFRESH_INTERVAL: Long = (303600 / 2) * 1000L // 30 minutes
    }
}