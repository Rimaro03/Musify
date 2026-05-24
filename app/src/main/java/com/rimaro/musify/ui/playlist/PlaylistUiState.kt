package com.rimaro.musify.ui.playlist

import com.rimaro.musify.domain.model.FirestorePlaylist
import com.rimaro.musify.domain.model.Track


sealed class PlaylistUiState {
    object Idle : PlaylistUiState()
    object Loading : PlaylistUiState()
    data class Success(
        val playlist: FirestorePlaylist,
        val trackList: List<Track>
    ) : PlaylistUiState()
    data class Error(val message: String) : PlaylistUiState()
}