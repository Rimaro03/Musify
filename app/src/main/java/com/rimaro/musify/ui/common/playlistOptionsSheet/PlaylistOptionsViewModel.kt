package com.rimaro.musify.ui.common.playlistOptionsSheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestorePlaylistRepo: FirestorePlaylistRepo
): ViewModel() {
    val playlistId: String? = savedStateHandle["playlistId"]

    private val _uiState = MutableStateFlow<FirestorePlaylist?>(null)
    val uiState: StateFlow<FirestorePlaylist?> = _uiState

    init {
        viewModelScope.launch {
            if (playlistId != null) {
                val playlist = firestorePlaylistRepo.getPlaylist(playlistId)
                _uiState.update { playlist }
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            playlistId?.let {
                firestorePlaylistRepo.deletePlaylist(it)
            }
        }
    }
}