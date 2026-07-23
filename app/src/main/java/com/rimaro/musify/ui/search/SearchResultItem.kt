package com.rimaro.musify.ui.search

import com.rimaro.musify.domain.model.DeezerAlbum
import com.rimaro.musify.domain.model.DeezerArtist
import com.rimaro.musify.ui.common.model.TrackUiModel

sealed class SearchResultItem {
    data class TrackItem(val trackModel: TrackUiModel) : SearchResultItem()
    data class AlbumItem(val album: DeezerAlbum) : SearchResultItem()
    data class ArtistItem(val artist: DeezerArtist) : SearchResultItem()
}