package com.rimaro.musify.ui.search

import com.rimaro.musify.domain.dto.DeezerAlbum
import com.rimaro.musify.domain.dto.DeezerArtist
import com.rimaro.musify.domain.dto.DeezerTrack

sealed class SearchResultItem {
    data class TrackItem(val track: DeezerTrack) : SearchResultItem()
    data class AlbumItem(val album: DeezerAlbum) : SearchResultItem()
    data class ArtistItem(val artist: DeezerArtist) : SearchResultItem()
}