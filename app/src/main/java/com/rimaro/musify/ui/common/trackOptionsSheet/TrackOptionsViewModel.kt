package com.rimaro.musify.ui.common.trackOptionsSheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.data.remote.deezer.dto.toTrack
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.data.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.domain.model.TrackUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackOptionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    deezerTrackRepository: DeezerRepository,
    private val likedTracksRepo: FirestoreLikedTracksRepo,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    playerController: PlayerController
) : ViewModel() {
    private val trackId: Long = checkNotNull(savedStateHandle["trackId"])
    val playlistId: String? = savedStateHandle["playlistId"]

    val uiState: StateFlow<TrackUiModel?> = combine(
        flow { emit(deezerTrackRepository.getTrackById(trackId)) },
        likedTracksRepo.likedTracks,
        playerController.currentTrack
    ) { deezerTrack, likedTracks, currentTrack ->
        TrackUiModel(
            track = deezerTrack.toTrack(null, null),
            isLiked = likedTracks.map { it.trackId }.contains(deezerTrack.id),
            isPlaying = currentTrack?.id == deezerTrack.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        null
    )

    fun toggleLike(trackId: Long) = viewModelScope.launch {
        likedTracksRepo.toggleLike(trackId)
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) = viewModelScope.launch {
        if(playlistId == "__liked__") likedTracksRepo.toggleLike(trackId)
        else firestorePlaylistRepo.removeTrack(playlistId, trackId)
    }
}