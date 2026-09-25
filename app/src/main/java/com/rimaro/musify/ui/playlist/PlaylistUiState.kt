package com.rimaro.musify.ui.playlist

import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.domain.model.TrackUiModel

sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    data class Success(
        val playlist: FirestorePlaylist,
        val trackList: List<TrackUiModel>
    ) : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}