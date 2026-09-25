package com.rimaro.musify.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rimaro.musify.data.local.db.dao.TrackDao
import com.rimaro.musify.data.local.extractor.TrackUrlResolver
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okio.IOException

@HiltWorker
class UrlRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val trackDao: TrackDao,
    private val trackUrlResolver: TrackUrlResolver
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        /*
        * 1. Get tracks expiring in time range
        * 2. For each track, refresh the URL
        * 3. Update the database with the new URL
        * */

        try {
            val expiringTracks = trackDao.getTracksToRefresh(
                threshold = System.currentTimeMillis() + REFRESH_INTERVAL,
            )
            expiringTracks.forEach { track ->
                val (newUrl, _) = trackUrlResolver.getFreshUrl(track.id, track.sourceUrl!!)

                if(newUrl.isNotBlank()) {
                    trackDao.upsert(track.copy(streamUrl = newUrl))
                }
            }
        } catch (e: IOException) {
            Log.d("UrlRefreshWorker", "Error refreshing tracks, retrying", e)
            return Result.retry()
        }
        catch (e: Exception) {
            Log.d("UrlRefreshWorker", "Error refreshing tracks", e)
            return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val REFRESH_INTERVAL: Long = 30 * 60 * 1000L
    }
}