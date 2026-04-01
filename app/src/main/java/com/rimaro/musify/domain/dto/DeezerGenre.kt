package com.rimaro.musify.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerGenre (
    val id: Int,
    val name: String,
    val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    val type: String = "genre")