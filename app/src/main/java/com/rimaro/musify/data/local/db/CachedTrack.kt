package com.rimaro.musify.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tracks")
data class CachedTrack (
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val sourceUrl: String? = null,
    val expiresAt: Long,
    val lastPlayedAt: Long,
    val failureCount: Int = 0,
    val nextRetryAt: Long = 0,
    val status: TrackStatus = TrackStatus.ACTIVE
)