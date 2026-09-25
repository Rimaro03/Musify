package com.rimaro.musify.ui.common.addToPlaylistSheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.domain.model.AddToPlaylistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val trackId: Long = checkNotNull(savedStateHandle["trackId"])

    val uiState: StateFlow<AddToPlaylistUiState?> = firestorePlaylistRepo
        .observeUserPlaylists()
        .map { userPlaylists ->
            AddToPlaylistUiState.Success(
                items = userPlaylists.map {
                    AddToPlaylistItem(
                        id = it.id,
                        name = it.name,
                        coverUrl = it.thumbnailPath,
                        containsTrack = it.tracks.any { track -> track.trackId == trackId }
                    )
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
}