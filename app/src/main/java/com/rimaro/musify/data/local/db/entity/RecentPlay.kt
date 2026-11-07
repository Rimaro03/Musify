package com.rimaro.musify.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "recently_played",
    foreignKeys = [ForeignKey(
        entity = TrackMetadata::class,
        parentColumns = ["trackId"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class RecentPlay (
    @PrimaryKey val trackId: Long,
    val playedAt: Long
)