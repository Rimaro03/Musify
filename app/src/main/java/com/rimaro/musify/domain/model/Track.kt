package com.rimaro.musify.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Track (
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val durationMs: Long,
    val genre: String?,
    val artworkUrl: String?,
    val albumId: Long?,
    var streamUrl: String?,
    val sourceUrl: String?,
    var previewUrl: String?
) : Parcelable

fun Track.toFirestoreTrack(): FirestoreTrack = FirestoreTrack(
    title = title,
    trackId = id,
    albumId = albumId,
    artist = artist,
    artistId = artistId,
    artworkUrl = artworkUrl,
    duration = (durationMs/1000L).toInt(),
    genres = genre,
    previewUrl = previewUrl
)