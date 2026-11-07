package com.rimaro.musify.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.local.db.entity.toTrack
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.data.repository.RecentPlayedRepository
import com.rimaro.musify.data.repository.TrackMetadataRepository
import com.rimaro.musify.data.repository.audio_url.AudioUrlRepository
import com.rimaro.musify.data.repository.audio_url.ResolutionState
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.TrackUiModel
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val playerController: PlayerController,
    private val firestoreLikedTracksRepo: FirestoreLikedTracksRepo,
    private val audioUrlRepository: AudioUrlRepository,
    private val queueManager: QueueManager,
    private val recentPlayedRepository: RecentPlayedRepository
) : AndroidViewModel(application) {
    val recentPlayedTracks = recentPlayedRepository.recentPlayed

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    private val currentTrack: Flow<Track?> = playerController.currentTrack
    private val currPlaylistId: MutableStateFlow<String?> = MutableStateFlow(null)
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
    private val audioTrackUrls: StateFlow<Map<Long, ResolutionState>> = audioUrlRepository.resolutionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val uiState: StateFlow<LibraryUiState> =
        combine(currentTrack, currPlaylistId, firestoreLikedTracksRepo.likedTracks,
            audioTrackUrls, recentPlayedRepository.recentPlayed)
        { currTrack, currPlaylistId, likedTracks, trackUrls, recentPlayed ->
            val thisPlaylistActive = currPlaylistId == playingPlaylistId.value
            val recentTracks = recentPlayed.map { track ->
                val resolutionState = trackUrls[track.id]
                TrackUiModel(
                    track = track.copy(
                        streamUrl = if(resolutionState is ResolutionState.Success) {
                            resolutionState.streamUrl
                        } else null
                    ),
                    isPlaying = thisPlaylistActive && track.id == currTrack?.id,
                    isLiked = likedTracks
                        .map{ it.trackId }
                        .contains(track.id)
                )
            }
            LibraryUiState.Success(recentPlayedTracks = recentTracks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            LibraryUiState.Loading
        )

    fun playTrack(track: Track) {
        if(uiState.value is LibraryUiState.Success) {
            playerController.setPlayingPlaylistId(currPlaylistId.value)
            playerController.clearQueue()
            queueManager.loadQueue(listOf(track), shuffleEnabled.value)
        }
    }

    fun unlikeTrack(track: Track) = viewModelScope.launch {
        firestoreLikedTracksRepo.toggleLike(track.id)
    }
}
