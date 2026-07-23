package com.rimaro.musify.domain.model


data class FirestorePlaylist(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val tracks: List<FirestoreTrack> = emptyList(),
    val thumbnailPath: String = ""
)
