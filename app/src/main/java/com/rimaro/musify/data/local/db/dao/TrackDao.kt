package com.rimaro.musify.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.rimaro.musify.data.local.db.entity.CachedTrack

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
    """)
    suspend fun getTracksToRefresh(
        threshold: Long,
    ): List<CachedTrack>

    @Query("UPDATE cached_tracks SET lastPlayedAt = :time WHERE id = :trackId")
    suspend fun updateLastPlayed(trackId: String, time: Long = System.currentTimeMillis())
}