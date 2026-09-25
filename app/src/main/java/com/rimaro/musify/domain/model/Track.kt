package com.rimaro.musify.domain.model

import android.os.Parcelable
import com.rimaro.musify.data.local.db.dao.TrackMetadataDao
import com.rimaro.musify.data.local.db.entity.TrackMetadata
import com.rimaro.musify.data.remote.firestore.model.FirestoreTrack
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

// TODO: remove this
fun Track.toFirestoreTrack(): FirestoreTrack = FirestoreTrack(
    title = title,
    trackId = id,
    albumId = albumId,
    artist = artist,
    artistId = artistId,
    artworkUrl = artworkUrl,
    duration = (durationMs / 1000L).toInt(),
    genres = genre,
    previewUrl = previewUrl
)

fun Track.toTrackMetadata(): TrackMetadata = TrackMetadata(
    trackId = id,
    title = title,
    artist = artist,
    artistId = artistId,
    durationMs = durationMs,
    genre = genre ?: "",
    artworkUrl = artworkUrl ?: "",
    albumId = albumId ?: 0L,
    previewUrl = previewUrl ?: ""
)