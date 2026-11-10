package com.rimaro.musify.ui.library.followedArtists

import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist

sealed class ArtistUiState {
    object Idle : ArtistUiState()
    object Loading : ArtistUiState()
    data class Error(val msg: String) : ArtistUiState()
    data class Success(val artists: List<DeezerArtist>) : ArtistUiState()
}