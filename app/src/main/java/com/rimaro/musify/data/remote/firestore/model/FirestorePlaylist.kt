package com.rimaro.musify.data.remote.firestore.model


data class FirestorePlaylist(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val tracks: List<FirestoreTrack> = emptyList(),
    val thumbnailPath: String = ""
)
