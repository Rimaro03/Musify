package com.rimaro.musify.data.remote.firestore.model

data class FirestorePlaylist(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val tracks: List<Long> = emptyList(),
    val thumbnailPath: String = ""
)
