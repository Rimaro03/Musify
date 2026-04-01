package com.rimaro.musify.domain.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerArtist (
    val id: Long,
    val name: String,
    val link: String? = null,
    val share: String? = null,
    val picture: String? = null,
    @SerialName("picture_small") val pictureSmall: String? = null,
    @SerialName("picture_medium") val pictureMedium: String? = null,
    @SerialName("picture_big") val pictureBig: String? = null,
    @SerialName("picture_xl") val pictureXl: String? = null,
    @SerialName("nb_album") val nbAlbum: Int? = null,
    @SerialName("nb_fan") val nbFan: Int? = null,
    val radio: Boolean? = null,
    val tracklist: String? = null,
    val type: String = "artist"
)