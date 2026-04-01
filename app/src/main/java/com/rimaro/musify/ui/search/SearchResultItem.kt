package com.rimaro.musify.ui.search

import androidx.recyclerview.widget.DiffUtil
import com.rimaro.musify.domain.dto.DeezerAlbum
import com.rimaro.musify.domain.dto.DeezerArtist
import com.rimaro.musify.domain.dto.DeezerTrack

sealed class SearchResultItem {
    data class TrackItem(val track: DeezerTrack) : SearchResultItem()
    data class AlbumItem(val album: DeezerAlbum) : SearchResultItem()
    data class ArtistItem(val artist: DeezerArtist) : SearchResultItem()

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchResultItem>() {
            override fun areItemsTheSame(old: SearchResultItem, new: SearchResultItem) =
                when (old) {
                    is TrackItem if new is TrackItem -> old.track.id == new.track.id
                    is ArtistItem if new is ArtistItem -> old.artist.id == new.artist.id
                    is AlbumItem if new is AlbumItem -> old.album.id == new.album.id
                    else -> false
                }

            override fun areContentsTheSame(old: SearchResultItem, new: SearchResultItem) =
                old == new
        }
    }

}