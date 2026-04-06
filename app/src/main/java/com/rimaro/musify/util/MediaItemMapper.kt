package com.rimaro.musify.util

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rimaro.musify.domain.model.DeezerTrack

object MediaItemMapper {

    fun fromTrack(track: DeezerTrack): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUrl?.toUri())
            .setDurationMs(track.durationMs)
            .setGenre(track.genre)
            .setTrackNumber(track.trackNumber)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(track.streamUrl.toUri())
                    .build()
            )
            .build()
    }

    fun fromTracks(tracks: List<DeezerTrack>): List<MediaItem> = tracks.map { fromTrack(it) }

    fun toTrack(mediaItem: MediaItem): DeezerTrack {
        val meta = mediaItem.mediaMetadata
        return DeezerTrack(
            id          = mediaItem.mediaId,
            title       = meta.title?.toString()       ?: "Unknown title",
            artist      = meta.artist?.toString()      ?: "Unknown artist",
            album       = meta.albumTitle?.toString()  ?: "Unknown album",
            artworkUrl  = meta.artworkUri?.toString(),
            durationMs  = meta.durationMs              ?: 0L,
            genre       = meta.genre?.toString(),
            trackNumber = meta.trackNumber,
            streamUrl   = mediaItem.requestMetadata.mediaUri?.toString() ?: ""
        )
    }
}