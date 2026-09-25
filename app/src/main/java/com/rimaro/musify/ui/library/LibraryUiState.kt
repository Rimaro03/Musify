package com.rimaro.musify.ui.library

import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist

sealed class LibraryUiState {
    object Idle : LibraryUiState()
    object Loading : LibraryUiState()
    data class Success(val res: List<FirestorePlaylist>) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}