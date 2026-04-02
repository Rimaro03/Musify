package com.rimaro.musify.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeezerGenreRes (
    val data: List<DeezerGenre>,
    val total: Int? = null,
    val next: String? = null
)
