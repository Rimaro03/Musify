package com.rimaro.musify.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TrackDao {

    @Query("SELECT * FROM cached_tracks")
    suspend fun getEverything(): List<CachedTrack>

    @Query("SELECT * FROM cached_tracks WHERE id = :trackId")
    suspend fun getTrack(trackId: String): CachedTrack?

    @Upsert
    suspend fun upsert(track: CachedTrack)

    @Delete
    suspend fun delete(track: CachedTrack)

    @Query("""
        SELECT * FROM cached_tracks 
        WHERE expiresAt < :threshold
        AND status = 'ACTIVE'
        AND (failureCount < 5 AND nextRetryAt < :now)
    """)
    suspend fun getTracksToRefresh(
        threshold: Long,       // expiring within the next job window
        now: Long = System.currentTimeMillis()
    ): List<CachedTrack>

    @Query("UPDATE cached_tracks SET lastPlayedAt = :time WHERE id = :trackId")
    suspend fun updateLastPlayed(trackId: String, time: Long = System.currentTimeMillis())

    @Query("UPDATE cached_tracks SET status = :status WHERE id = :trackId")
    suspend fun updateStatus(trackId: String, status: TrackStatus)

    @Query("UPDATE cached_tracks SET failureCount = :count, nextRetryAt = :nextRetryAt WHERE id = :trackId")
    suspend fun updateFailure(trackId: String, count: Int, nextRetryAt: Long)

    @Query("""
    UPDATE cached_tracks 
    SET status = 'COLD' 
    WHERE lastPlayedAt < :coldThreshold 
    AND status = 'ACTIVE'
""")
    suspend fun markColdTracks(coldThreshold: Long)
}
