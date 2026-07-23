package com.rimaro.musify.ui.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.controller.PreviewPlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import com.rimaro.musify.ui.common.PlayButtonState
import com.rimaro.musify.ui.common.model.TrackUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    application: Application,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val deezerRepository: DeezerRepository,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController,
    private val previewPlayerController: PreviewPlayerController,
) : AndroidViewModel(application) {
    private val _playlistRawState: MutableStateFlow<PlaylistUiState> = MutableStateFlow(
        PlaylistUiState.Idle)
    private val currentTrack: Flow<Track?> = playerController.currentTrack
    private val currPlaylistId: MutableStateFlow<String?> = MutableStateFlow(checkNotNull(savedStateHandle["playlistId"]))

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId

    private var latestLikedTrackIds: List<Long> = emptyList()

    var playlistUiState: Flow<PlaylistUiState> = combine(_playlistRawState, currentTrack, currPlaylistId)
    { rawState, currTrack, currPlaylistId ->
        when(rawState) {
            is PlaylistUiState.Success -> {
                val thisPlaylistActive = currPlaylistId == playingPlaylistId.value
                PlaylistUiState.Success(
                    playlist = rawState.playlist,
                    trackList = rawState.trackList.map { trackModel ->
                        trackModel.copy(isPlaying = thisPlaylistActive && trackModel.track.id == currTrack?.id)
                    }
                )
            }
            is PlaylistUiState.Idle -> PlaylistUiState.Idle
            is PlaylistUiState.Loading -> PlaylistUiState.Loading
            is PlaylistUiState.Error -> PlaylistUiState.Error(rawState.message)
        }
    }

    init {
        retrieveTrackIds(currPlaylistId.value)
    }

    val playButtonState: StateFlow<PlayButtonState> = combine(
        playerState, isPlaying, playingPlaylistId
    ) {state, playing, activeId ->
        when {
            state == Player.STATE_BUFFERING && activeId == currPlaylistId.value -> PlayButtonState.Buffering
            playing && activeId == currPlaylistId.value -> PlayButtonState.PlayingThis
            else -> if (activeId == currPlaylistId.value) PlayButtonState.Idle else PlayButtonState.PlayingOther
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayButtonState.Idle)

    fun retrieveTrackIds(playlistId: String?) {
        if (playlistId == null) return
        viewModelScope.launch {
            _playlistRawState.value = PlaylistUiState.Loading
            val firestorePlaylist = firestorePlaylistDao.getPlaylist(playlistId)
            if(firestorePlaylist == null) {
                _playlistRawState.value = PlaylistUiState.Error("Could not retrieve playlist")
                return@launch
            }

            val firestoreTracks = firestorePlaylist.tracks
            val trackUiModels = firestoreTracks.map {
                TrackUiModel(track = it.toTrack())
            }
            _playlistRawState.value = PlaylistUiState.Success(firestorePlaylist, trackUiModels)
            applyLikedTracks()

            fetchStreamUrl(trackUiModels).collect { fetchedTrack ->
                _playlistRawState.update { state ->
                    if (state is PlaylistUiState.Success) {
                        val updatedTracks = state.trackList.map { trackModel ->
                            if (trackModel.track.id == fetchedTrack.track.id) fetchedTrack else trackModel
                        }
                        state.copy(trackList = updatedTracks)
                    } else state
                }
            }
            applyLikedTracks()
        }
    }

    private fun fetchStreamUrl(tracks: List<TrackUiModel>): Flow<TrackUiModel> = channelFlow {
        val semaphore = Semaphore(5)
        tracks.map { trackModel ->
            async {
                semaphore.withPermit {
                    val fetchedTrack = trackUrlResolver.resolve(trackModel.track)
                    fetchedTrack?.let { send(TrackUiModel(track = it, isLiked = trackModel.isLiked)) }
                }
            }
        }.awaitAll()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            track.streamUrl?.let {
                playerController.playTracks(listOf(track), currPlaylistId.value)
            }
        }
    }

    fun playPreview(track: Track) {
        playerController.pause()
        track.previewUrl?.let {
            previewPlayerController.playPreview(track.id.toString(), it)
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

    fun onLikedTracksChange(likedTrackIds: List<Long>) {
        latestLikedTrackIds = likedTrackIds
        applyLikedTracks()
    }

    fun applyLikedTracks() {
        _playlistRawState.update { state ->
            if(state is PlaylistUiState.Success) {
                val updatedTracks = state.trackList.map { trackModel ->
                    trackModel.copy(isLiked = trackModel.track.id in latestLikedTrackIds)
                }
                state.copy(trackList = updatedTracks)
            }
            else state
        }
    }

    private fun playPlaylist() {
        if(_playlistRawState.value is PlaylistUiState.Success && currPlaylistId.value != null) {
            val tracksToPlay = (_playlistRawState.value as PlaylistUiState.Success).trackList.map { it.track }
            playerController.playPlaylist(tracksToPlay, currPlaylistId.value!!)
        }
    }

}