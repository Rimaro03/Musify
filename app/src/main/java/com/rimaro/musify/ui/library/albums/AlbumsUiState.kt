package com.rimaro.musify.ui.library.albums

import com.rimaro.musify.data.remote.deezer.dto.DeezerAlbum

sealed class AlbumsUiState {
    object Idle : AlbumsUiState()
    object Loading : AlbumsUiState()
    data class Error(val msg: String) : AlbumsUiState()
    data class Success(val albums: List<DeezerAlbum>) : AlbumsUiState()
}