package com.rimaro.musify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchRes (
    val data: List<DeezerTrack>,
    val total: Int,
    val next: String? = null
)