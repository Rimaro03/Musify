package com.rimaro.musify.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerContributor (
    val id: Long,
    val name: String,
    val link: String,
    val share: String,
    val picture: String,
    @SerialName("picture_small") val pictureSmall: String,
    @SerialName("picture_medium") val pictureMedium: String,
    @SerialName("picture_big") val pictureBig: String,
    @SerialName("picture_xl") val pictureXl: String,
    val radio: Boolean,
    val tracklist: String,
    val type: String,
    val role: String // e.g., "Main", "Featured"
)