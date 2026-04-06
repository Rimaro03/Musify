package com.rimaro.musify.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeezerChartArtistRes (
    val data: List<DeezerArtist>,
    val total: Int,
    val next: String? = null
)