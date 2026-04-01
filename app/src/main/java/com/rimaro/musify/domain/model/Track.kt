package com.rimaro.musify.domain.model

data class Track (
    val id: String,
    val title: String,
    val artistName: String,
    val albumTitle: String,
    val durationText: String,
    val thumbnailUrl: String?,
    val score: Int
)