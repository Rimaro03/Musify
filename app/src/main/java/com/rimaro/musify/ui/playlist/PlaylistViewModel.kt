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
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.controller.PreviewPlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    application: Application,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController,
    private val previewPlayerController: PreviewPlayerController,
    private val likedTracksRepo: FirestoreLikedTracksRepo,
    private val queueManager: QueueManager
) : AndroidViewModel(application) {
    private val _playlistState: MutableStateFlow<PlaylistUiState> = MutableStateFlow(
        PlaylistUiState.Idle)
    private val currentTrack: Flow<Track?> = playerController.currentTrack
    private val currPlaylistId: MutableStateFlow<String?> = MutableStateFlow(checkNotNull(savedStateHandle["playlistId"]))

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
    private val audioTrackUrls: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    var uiState: Flow<PlaylistUiState> =
        combine(_playlistState, currentTrack, currPlaylistId, likedTracksRepo.likedTracks, audioTrackUrls)
        { rawState, currTrack, currPlaylistId, likedTrackIds, trackUrls ->
            when(rawState) {
                is PlaylistUiState.Success -> {
                    val thisPlaylistActive = currPlaylistId == playingPlaylistId.value
                    PlaylistUiState.Success(
                        playlist = rawState.playlist,
                        trackList = rawState.trackList.map { trackModel ->
                            trackModel.copy(
                                track = trackModel.track.copy(
                                    streamUrl = trackUrls[trackModel.track.id.toString()]
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
            _playlistState.value = PlaylistUiState.Loading
            val firestorePlaylist = firestorePlaylistRepo.getPlaylist(playlistId)
            if(firestorePlaylist == null) {
                _playlistState.value = PlaylistUiState.Error("Could not retrieve playlist")
                return@launch
            }

            val firestoreTracks = firestorePlaylist.tracks
            val trackUiModels = firestoreTracks.map {
                TrackUiModel(track = it.toTrack())
            }
            _playlistState.value = PlaylistUiState.Success(firestorePlaylist, trackUiModels)

//            fetchStreamUrl(trackUiModels).collect { fetchedTrack ->
//                audioTrackUrls.value += fetchedTrack
//                // TODO: move this to a repo
//            }
        }
    }

//    private fun fetchStreamUrl(tracks: List<TrackUiModel>): StateFlow<Map<String, String>> = channelFlow {
//        val semaphore = Semaphore(5)
//        tracks.map { trackModel ->
//            async {
//                semaphore.withPermit {
//                    val fetchedTrack = trackUrlResolver.resolve(trackModel.track)
//                    fetchedTrack?.let {
//                        if(it.streamUrl != null) {
//                            send(mapOf(Pair(it.id.toString(), it.streamUrl!!)))
//                        }
//                    }
//                }
//            }
//        }.awaitAll()
//    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())


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

    private fun playPlaylist() {
        if(_playlistState.value is PlaylistUiState.Success && currPlaylistId.value != null) {
            playerController.setPlayingPlaylistId(currPlaylistId.value)
            val tracksToPlay = (_playlistState.value as PlaylistUiState.Success).trackList.map { it.track }
            queueManager.loadQueue(tracksToPlay, shuffleEnabled.value)
        }
    }

    /* TRACK LIKE/UNLIKE LOGIC */
    fun unlikeTrack(track: Track) = viewModelScope.launch {
        likedTracksRepo.removeTrack(track.id)
    }

}