package com.rimaro.musify.ui.common.addToPlaylistSheet

import com.rimaro.musify.domain.model.AddToPlaylistItem

sealed class AddToPlaylistUiState {
    data class Success(val items: List<AddToPlaylistItem>) : AddToPlaylistUiState()
    object Loading : AddToPlaylistUiState()
    object Error : AddToPlaylistUiState()
}