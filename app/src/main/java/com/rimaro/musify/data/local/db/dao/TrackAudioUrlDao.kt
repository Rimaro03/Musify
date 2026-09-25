package com.rimaro.musify.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.rimaro.musify.data.local.db.entity.TrackAudioUrl

@Dao
interface TrackAudioUrlDao {

    @Query("SELECT * FROM track_audio_url")
    suspend fun getEverything(): List<TrackAudioUrl>

    @Query("SELECT * FROM track_audio_url WHERE id = :trackId")
    suspend fun getTrack(trackId: String): TrackAudioUrl?

    @Upsert
    suspend fun upsert(track: TrackAudioUrl)

    @Delete
    suspend fun delete(track: TrackAudioUrl)

    @Query("""
        SELECT * FROM track_audio_url 
        WHERE expiresAt < :threshold
    """)
    suspend fun getTracksToRefresh(
        threshold: Long,
    ): List<TrackAudioUrl>

    @Query("UPDATE track_audio_url SET lastPlayedAt = :time WHERE id = :trackId")
    suspend fun updateLastPlayed(trackId: String, time: Long = System.currentTimeMillis())
}