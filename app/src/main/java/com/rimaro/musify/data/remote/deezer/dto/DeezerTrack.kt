package com.rimaro.musify.data.remote.deezer.dto

import com.rimaro.musify.data.local.db.entity.TrackMetadata
import com.rimaro.musify.data.repository.TrackMetadataRepository
import com.rimaro.musify.domain.model.Track
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerTrack (
    val id: Long,
    val readable: Boolean? = null,
    val title: String,
    @SerialName("title_short") val titleShort: String? = null,
    @SerialName("title_version") val titleVersion: String? = null,
    val isrc: String? = null,
    val link: String? = null,
    val duration: Int,
    @SerialName("track_position") val trackPosition: Int? = null,
    @SerialName("disk_number") val diskNumber: Int? = null,
    val rank: Int? = null,
    @SerialName("explicit_lyrics") val explicitLyrics: Boolean? = null,
    @SerialName("explicit_content_lyrics") val explicitContentLyrics: Int? = null,
    @SerialName("explicit_content_display") val explicitContentDisplay: Int? = null,
    val preview: String? = null,
    val bpm: Float? = null,
    val gain: Float? = null,
    @SerialName("available_countries") val availableCountries: List<String>? = null,
    val contributor: List<DeezerContributor>? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null, // Note: recursive reference possible in some endpoints
    val type: String = "track"
)

fun DeezerTrack.toTrack(
    streamUrl: String? = null,
    sourceUrl: String? = null,
): Track = Track(
    id = id,
    title = title,
    artist = artist?.name ?: "Unknown Artist",
    artistId = artist?.id ?: 0L,
    durationMs = duration * 1000L,
    genre = album?.genres?.joinToString(", "),
    artworkUrl = album?.coverXl,
    albumId = album?.id ?: 0L,
    streamUrl = streamUrl,
    sourceUrl = sourceUrl,
    previewUrl = preview
)

fun DeezerTrack.toTrackMetadata(): TrackMetadata = TrackMetadata(
    trackId = id,
    title = title,
    artist = artist?.name ?: "Unknown Artist",
    artistId = artist?.id ?: 0L,
    durationMs = duration * 1000L,
    genre = album?.genres?.joinToString(", ") ?: "",
    artworkUrl = album?.coverXl ?: "",
    albumId = album?.id ?: 0L,
    previewUrl = preview ?: ""
)