package com.rimaro.musify.ui.library.artists

import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist

sealed class ArtistsUiState {
    object Idle : ArtistsUiState()
    object Loading : ArtistsUiState()
    data class Error(val msg: String) : ArtistsUiState()
    data class Success(val artists: List<DeezerArtist>) : ArtistsUiState()
}