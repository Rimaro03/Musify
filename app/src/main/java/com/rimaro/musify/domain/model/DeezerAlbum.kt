package com.rimaro.musify.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerAlbum (
    val id: Long,
    val title: String,
    val upc: String? = null,
    val link: String? = null,
    val share: String? = null,
    val cover: String,
    @SerialName("cover_small") val coverSmall: String,
    @SerialName("cover_medium") val coverMedium: String,
    @SerialName("cover_big") val coverBig: String,
    @SerialName("cover_xl") val coverXl: String,
    @SerialName("md5_image") val md5Image: String,
    @SerialName("genre_id") val genreId: Int? = null,
    val genres: List<DeezerGenre>? = null,
    val label: String? = null,
    @SerialName("nb_tracks") val nbTracks: Int? = null,
    val duration: Int? = null,
    val fans: Int? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("record_type") val recordType: String? = null,
    val available: Boolean? = null,
    val tracklist: String,
    @SerialName("explicit_lyrics") val explicitLyrics: Boolean? = null,
    @SerialName("explicit_content_lyrics") val explicitContentLyrics: Int? = null,
    @SerialName("explicit_content_display") val explicitContentDisplay: Int? = null,
    val artist: DeezerArtist? = null,
    val type: String = "album",
    // Only in full response
    val tracks: List<DeezerTrack>? = null,
    val contributors: List<DeezerContributor>? = null
)