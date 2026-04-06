package com.rimaro.musify.domain.model

data class Track (
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val genre: String?,
    val artworkUrl: String?,
    val streamUrl: String,
)