package com.rimaro.musify.ui.library.playlists

import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist

sealed class PlaylistsUiState {
    object Idle : PlaylistsUiState()
    object Loading : PlaylistsUiState()
    data class Success(val res: List<FirestorePlaylist>) : PlaylistsUiState()
    data class Error(val message: String) : PlaylistsUiState()
}