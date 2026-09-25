package com.rimaro.musify.domain.model

data class AddToPlaylistItem (
    val id: String,
    val name: String,
    val coverUrl: String,
    val containsTrack: Boolean
)