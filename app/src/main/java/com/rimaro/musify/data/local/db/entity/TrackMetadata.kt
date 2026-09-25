package com.rimaro.musify.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rimaro.musify.domain.model.Track

@Entity(tableName = "track_metadata")
data class TrackMetadata (
    @PrimaryKey val trackId: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val durationMs: Long,
    val genre: String,
    val artworkUrl: String,
    val albumId: Long,
    val previewUrl: String
)

fun TrackMetadata.toTrack(): Track = Track(
    id = trackId,
    title = title,
    artist = artist,
    artistId = artistId,
    durationMs = durationMs,
    genre = genre,
    artworkUrl = artworkUrl,
    albumId = albumId,
    previewUrl = previewUrl,
    sourceUrl = null,
    streamUrl = null
)