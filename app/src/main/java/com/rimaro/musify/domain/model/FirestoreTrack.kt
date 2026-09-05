package com.rimaro.musify.domain.model

import com.google.firebase.firestore.DocumentId

data class FirestoreTrack (
    @DocumentId
    val documentId: String = "",
    val title: String = "",
    val trackId: Long = 0L,
    val albumId: Long? = 0L,
    val artist: String? = "",
    val artistId: Long? = 0L,
    val artworkUrl: String? = "",
    val duration: Int = 0,
    val genres: String? = "",
    val previewUrl: String? = "",
)

fun FirestoreTrack.toTrack(
    streamUrl: String? = null,
    sourceUrl: String? = null,
): Track = Track(
    id = trackId,
    title = title,
    artist = artist ?: "Unknown Artist",
    artistId = artistId ?: 0L,
    durationMs = duration * 1000L,
    genre = genres,
    artworkUrl = artworkUrl,
    albumId = albumId ?: 0L,
    streamUrl = streamUrl,
    sourceUrl = sourceUrl,
    previewUrl = previewUrl
)