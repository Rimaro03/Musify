package com.rimaro.musify.ui.library

import com.rimaro.musify.domain.model.TrackUiModel

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    object Loading : LibraryUiState()
    data class Success(val recentPlayedTracks: List<TrackUiModel>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}