package com.rimaro.musify.util

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.rimaro.musify.domain.model.Track

object MediaItemMapper {

    fun fromTrack(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUrl?.toUri())
            .setDurationMs(track.durationMs)
            .setGenre(track.genre)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.streamUrl)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(track.streamUrl?.toUri())
                    .build()
            )
            .build()
    }

    fun fromTracks(tracks: List<Track>): List<MediaItem> = tracks.map { fromTrack(it) }

    fun toTrack(mediaItem: MediaItem): Track {
        val meta = mediaItem.mediaMetadata
        return Track(
            id          = mediaItem.mediaId.toLong(),
            title       = meta.title?.toString()       ?: "Unknown title",
            artist      = meta.artist?.toString()      ?: "Unknown artist",
            album       = meta.albumTitle?.toString()  ?: "Unknown album",
            artworkUrl  = meta.artworkUri?.toString(),
            durationMs  = meta.durationMs              ?: 0L,
            genre       = meta.genre?.toString(),
            streamUrl   = mediaItem.requestMetadata.mediaUri?.toString() ?: "",
            sourceUrl   = null
        )
    }
}