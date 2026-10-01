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

fun Track.toFirestoreTrack(): FirestoreTrack = FirestoreTrack(
    trackId = id,
)