package com.rimaro.musify.data.remote.deezer.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchTrackRes (
    val data: List<DeezerTrack>,
    val total: Int,
    val next: String? = null
)