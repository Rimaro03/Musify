package com.rimaro.musify.data.repository

import com.rimaro.musify.data.local.db.dao.RecentPlayDao
import com.rimaro.musify.data.local.db.entity.toTrack
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentPlayedRepository @Inject constructor(
    private val recentPlayDao: RecentPlayDao
) {
    val recentPlayed = recentPlayDao
        .observeRecentlyPlayed()
        .map { list ->
            list.map { it.toTrack() }
        }

    suspend fun recordPlay(trackId: Long) = recentPlayDao
        .recordPlay(trackId)
}