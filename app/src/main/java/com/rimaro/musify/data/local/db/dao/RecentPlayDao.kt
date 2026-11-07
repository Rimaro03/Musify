package com.rimaro.musify.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rimaro.musify.data.local.db.entity.RecentPlay
import com.rimaro.musify.data.local.db.entity.TrackMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentPlayDao {
    @Upsert suspend fun upsert(track: RecentPlay)

    @Query("""
        SELECT track_meta.* FROM track_metadata track_meta
        INNER JOIN recently_played recent ON recent.trackId = track_meta.trackId
        ORDER BY recent.playedAt DESC
        LIMIT 50
    """)
    fun observeRecentlyPlayed(): Flow<List<TrackMetadata>>

    @Query("""DELETE FROM recently_played WHERE trackId NOT IN
              (SELECT trackId FROM recently_played ORDER BY playedAt DESC LIMIT 50)""")
    suspend fun trim()

    @Transaction
    suspend fun recordPlay(trackId: Long) {
        upsert(RecentPlay(trackId = trackId, playedAt = System.currentTimeMillis()))
        trim()
    }
}