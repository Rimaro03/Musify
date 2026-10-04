package com.rimaro.musify.ui.library.likedTracks

import com.rimaro.musify.domain.model.TrackUiModel


sealed class LikedTracksUiState {
    object Idle : LikedTracksUiState()
    object Loading : LikedTracksUiState()
    data class Success(val tracks: List<TrackUiModel>) : LikedTracksUiState()
    data class Error(val message: String) : LikedTracksUiState()
}