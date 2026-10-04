package com.rimaro.musify.ui.library.likedTracks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.data.repository.TrackMetadataRepository
import com.rimaro.musify.data.repository.audio_url.AudioUrlRepository
import com.rimaro.musify.data.repository.audio_url.ResolutionState
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.TrackUiModel
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.ui.common.PlayButtonState
import com.rimaro.musify.ui.playlist.PlaylistUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedTracksViewModel @Inject constructor(
    private val firestoreLikedTracksRepo: FirestoreLikedTracksRepo,
    private val audioUrlRepository: AudioUrlRepository,
    private val playerController: PlayerController,
    private val trackMetadataRepository: TrackMetadataRepository,
    private val queueManager: QueueManager
) : ViewModel() {
    private val currentTrack: Flow<Track?> = playerController.currentTrack
    private val currPlaylistId: MutableStateFlow<String?> = MutableStateFlow("__liked__")

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
    private val audioTrackUrls: StateFlow<Map<Long, ResolutionState>> = audioUrlRepository.resolutionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val playButtonState: StateFlow<PlayButtonState> = combine(
        playerState, isPlaying, playingPlaylistId
    ) { state, playing, activeId ->
        when {
            state == Player.STATE_BUFFERING
                    && activeId == currPlaylistId.value -> PlayButtonState.Buffering
            playing && activeId == currPlaylistId.value -> PlayButtonState.PlayingThis
            else -> if (activeId == currPlaylistId.value) PlayButtonState.Idle else PlayButtonState.PlayingOther
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayButtonState.Idle)


    val uiState: StateFlow<LikedTracksUiState> =
        combine(currentTrack, currPlaylistId, firestoreLikedTracksRepo.likedTracks, audioTrackUrls)
        { currTrack, currPlaylistId, likedTracks, trackUrls ->
            val thisPlaylistActive = currPlaylistId == playingPlaylistId.value
            val trackIds = likedTracks.map { it.trackId }
            trackMetadataRepository.getTracks(trackIds).fold(
                onSuccess = { tracks ->
                    val trackUiModels = tracks.map { track ->
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
                    LikedTracksUiState.Success(tracks = trackUiModels)
                },
                onFailure = {
                    LikedTracksUiState.Error("Could not retrieve liked tracks")
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            LikedTracksUiState.Loading
        )

    fun togglePlayButton() {
        if(playerState.value == Player.STATE_BUFFERING) return

        if(playerState.value == Player.STATE_READY) {
            if(playingPlaylistId.value == currPlaylistId.value) {
                playerController.togglePlayPause()
            } else {
                playerController.clearQueue()
                playTracks((uiState.value as LikedTracksUiState.Success).tracks.map { it.track })
            }
        }
        else {
            playerController.clearQueue()
            playTracks((uiState.value as LikedTracksUiState.Success).tracks.map { it.track })
        }
    }

    fun playFromTrack(track: Track) {
        val trackList = (uiState.value as LikedTracksUiState.Success).tracks
            .map { it.track }
        val trackPos = trackList.indexOfFirst { it.id == track.id }
        val tracksToPlay = trackList.subList(trackPos, trackList.size)
        playTracks(tracksToPlay)
    }

    private fun playTracks(tracks: List<Track>) {
        if(uiState.value is LikedTracksUiState.Success && currPlaylistId.value != null) {
            playerController.setPlayingPlaylistId(currPlaylistId.value)
            playerController.clearQueue()
            queueManager.loadQueue(tracks, shuffleEnabled.value)
        }
    }

    fun unlikeTrack(track: Track) = viewModelScope.launch {
        firestoreLikedTracksRepo.toggleLike(track.id)
    }
}