package com.rimaro.musify.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tracks")
data class CachedTrack (
    @PrimaryKey val id: String,
    val streamUrl: String,
    val sourceUrl: String? = null,
    val expiresAt: Long,
    val lastPlayedAt: Long,
)