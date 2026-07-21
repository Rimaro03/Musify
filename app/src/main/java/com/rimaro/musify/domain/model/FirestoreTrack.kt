package com.rimaro.musify.domain.model

data class FirestoreTrack (
    val title: String,
    val trackId: Long,
    val albumId: Long,
    val artist: String,
    val artistInt: Long,
    val artworkUrl: String,
    val duration: Int,
    val genres: String,
    val previewUrl: String,
)