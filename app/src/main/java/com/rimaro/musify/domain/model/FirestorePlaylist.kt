package com.rimaro.musify.domain.model


data class FirestorePlaylist(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val trackIds: List<Long> = emptyList(),
    val thumbnailPath: String = ""
)
