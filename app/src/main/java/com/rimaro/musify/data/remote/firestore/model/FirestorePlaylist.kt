package com.rimaro.musify.data.remote.firestore.model

import com.google.firebase.Timestamp

data class FirestorePlaylist(
    val id: String = "",
    val name: String = "",
    val tracks: List<Long> = emptyList(),
    val thumbnailPath: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
