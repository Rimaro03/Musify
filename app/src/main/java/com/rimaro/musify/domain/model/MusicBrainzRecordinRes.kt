package com.rimaro.musify.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class MusicBrainzRecordinRes(
    val created: String? = null,
    val count: Int? = null,
    val offset: Int? = null,
    val recordings: List<RecordingDTO> = emptyList()
)

@Serializable
data class RecordingDTO(
    val id: String,
    val score: String? = null,

    @SerialName("artist-credit-id")
    val artistCreditId: String? = null,

    val title: String? = null,
    val length: Long? = null,
    val video: Boolean?,
    val disambiguation: String? = null,

    @SerialName("artist-credit")
    val artistCredit: List<ArtistCreditDTO>? = null,

    @SerialName("release-group")
    val releaseGroup: ReleaseGroupDTO? = null,

    val releases: List<ReleaseDTO>? = null,
    val tags: List<TagDTO>? = null,
    val isrcs: List<String>? = null
)

@Serializable
data class ArtistCreditDTO(
    val name: String? = null,
    val artist: ArtistDTO? = null
)

@Serializable
data class ArtistDTO(
    val id: String,
    val name: String,
    @SerialName("sort-name")
    val sortName: String? = null,
    val disambiguation: String? = null
)

@Serializable
data class ReleaseDTO(
    val id: String,
    val title: String,
    val status: String? = null,
    val date: String? = null,
    val country: String? = null,

    @SerialName("release-group")
    val releaseGroup: ReleaseGroupDTO? = null,

    @SerialName("track-count")
    val trackCount: Int? = null,
)

@Serializable
data class ReleaseGroupDTO(
    val id: String, // Use this ID for the Cover Art Archive thumbnail!
    val title: String? = null,

    @SerialName("primary-type")
    val primaryType: String? = null
)

@Serializable
data class TagDTO(
    val count: Int? = null,
    val name: String
)

//fun RecordingDTO.toDomain(): Track {
//    val artistName = artistCredit?.firstOrNull()?.name ?: "Unknown Artist"
//
//    val albumTitle = releaseGroup?.title ?: "Single"
//
//    val imageUrl = releases?.get(0)?.releaseGroup?.id?.let { mbid ->
//        "https://coverartarchive.org/release-group/$mbid/front-250"
//    }
//
//    // Format Duration (ms to MM:SS)
//    val formattedDuration = length?.let { ms ->
//        val minutes = (ms / 1000) / 60
//        val seconds = (ms / 1000) % 60
//        String.format(Locale.getDefault(),"%d:%02d", minutes, seconds)
//    } ?: "--:--"
//
//    return Track(
//        id = this.id,
//        title = this.title ?: "Unknown title",
//        artistName = artistName,
//        albumTitle = albumTitle,
//        durationText = formattedDuration,
//        thumbnailUrl = imageUrl,
//        score = this.score?.toInt() ?: 0
//    )
//}