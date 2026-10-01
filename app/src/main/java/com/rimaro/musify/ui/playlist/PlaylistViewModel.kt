package com.rimaro.musify.ui.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.data.repository.TrackMetadataRepository
import com.rimaro.musify.data.repository.audio_url.AudioUrlRepository
import com.rimaro.musify.data.repository.audio_url.ResolutionState
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.ui.common.PlayButtonState
import com.rimaro.musify.domain.model.TrackUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    application: Application,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    private val playerController: PlayerController,
    private val likedTracksRepo: FirestoreLikedTracksRepo,
    private val queueManager: QueueManager,
    private val audioUrlRepository: AudioUrlRepository,
    private val trackMetadataRepository: TrackMetadataRepository
) : AndroidViewModel(application) {
    private val _playlistId = MutableStateFlow<String?>(savedStateHandle["playlistId"])
    val playlistId: StateFlow<String?> = _playlistId

    val deleted = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _playlistState: StateFlow<PlaylistUiState> = _playlistId
        .flatMapLatest { id ->
            if(id == null) flowOf(PlaylistUiState.Error("Invalid Playlist ID"))
            else {
                firestorePlaylistRepo.observePlaylistTracks(id)
                    .map { firestorePlaylist ->
                        val trackIds = firestorePlaylist.tracks
                        trackMetadataRepository.getTracks(trackIds).fold(
                            onSuccess = { tracks ->
                                val trackUiModels = tracks.map { TrackUiModel(track = it) }
                                PlaylistUiState.Success(firestorePlaylist, trackUiModels)
                            },
                            onFailure = {
                                PlaylistUiState.Error("Could not retrieve playlist")
                            })
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaylistUiState.Loading
        )

    private val currentTrack: Flow<Track?> = playerController.currentTrack
    private val currPlaylistId: MutableStateFlow<String?> = MutableStateFlow(checkNotNull(savedStateHandle["playlistId"]))

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

    var uiState: Flow<PlaylistUiState> =
        combine(_playlistState, currentTrack, currPlaylistId, likedTracksRepo.likedTracks, audioTrackUrls)
        { rawState, currTrack, currPlaylistId, likedTrackIds, trackUrls ->
            when(rawState) {
                is PlaylistUiState.Success -> {
                    val thisPlaylistActive = currPlaylistId == playingPlaylistId.value
                    PlaylistUiState.Success(
                        playlist = rawState.playlist,
                        trackList = rawState.trackList.map { trackModel ->
                            val resolutionState = trackUrls[trackModel.track.id]
                            trackModel.copy(
                                track = trackModel.track.copy(
                                    streamUrl = if(resolutionState is ResolutionState.Success) {
                                        resolutionState.streamUrl
                                    } else null
                                ),
                                isPlaying = thisPlaylistActive && trackModel.track.id == currTrack?.id,
                                isLiked = likedTrackIds
                                    .map{ it.trackId }
                                    .contains(trackModel.track.id)
                            )
                        }
                    )
                }
                is PlaylistUiState.Idle -> PlaylistUiState.Idle
                is PlaylistUiState.Loading -> PlaylistUiState.Loading
                is PlaylistUiState.Error -> PlaylistUiState.Error(rawState.message)
            }
        }

    fun playTrack(track: Track) {
        if(_playlistState.value is PlaylistUiState.Success && currPlaylistId.value != null) {
            playerController.setPlayingPlaylistId(currPlaylistId.value)
            playerController.clearQueue()
            val trackList = (_playlistState.value as PlaylistUiState.Success).trackList
                .map { it.track }
            val trackPos = trackList.indexOfFirst { it.id == track.id }
            val tracksToPlay = trackList.subList(trackPos, trackList.size)

            queueManager.loadQueue(tracksToPlay, shuffleEnabled.value)
        }
    }

    fun toggleShuffle() = playerController.toggleShuffle()

    fun togglePlayButton() {
        if(playerState.value == Player.STATE_BUFFERING) return

        if(playerState.value == Player.STATE_READY) {
            if(playingPlaylistId.value == currPlaylistId.value) {
                playerController.togglePlayPause()
            } else {
                playerController.clearQueue()
                playPlaylist()
            }
        }
        else {
            playerController.clearQueue()
            playPlaylist()
        }
    }

    private fun playPlaylist() {
        if(_playlistState.value is PlaylistUiState.Success && currPlaylistId.value != null) {
            playerController.setPlayingPlaylistId(currPlaylistId.value)
            playerController.clearQueue()
            val tracksToPlay = (_playlistState.value as PlaylistUiState.Success).trackList.map { it.track }
            queueManager.loadQueue(tracksToPlay, shuffleEnabled.value)
        }
    }

    /* TRACK LIKE/UNLIKE LOGIC */
    fun unlikeTrack(track: Track) = viewModelScope.launch {
        likedTracksRepo.removeTrack(track.id)
    }

    fun playNext(track: Track) = viewModelScope.launch {
        val currTrack = playerController.currentTrack.value
        if(currTrack == null) {
            queueManager.loadQueue(listOf(track), playerController.shuffleEnabled.value)
        } else {
            queueManager.playNext(currTrack, track)
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            val id = _playlistId.value ?: return@launch
            try {
                _playlistId.value = null
                firestorePlaylistRepo.deletePlaylist(id)
                deleted.emit(true)
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "Error deleting playlist $playlistId: ${e.message}")
            }
        }
    }
}