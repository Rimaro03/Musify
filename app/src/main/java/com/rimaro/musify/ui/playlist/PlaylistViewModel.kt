package com.rimaro.musify.ui.playlist

import android.app.Application
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val deezerRepository: DeezerRepository,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController,
    private val previewPlayerController: PreviewPlayerController,
) : AndroidViewModel(application) {
    private val currPlaylistId: String = checkNotNull(savedStateHandle["playlistId"])

    private val _playlistUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val playlistUiState = _playlistUiState.asStateFlow()

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId

    init {
        retrieveTrackIds(currPlaylistId)
    }

    val playButtonState: StateFlow<PlayButtonState> = combine(
        playerState, isPlaying, playingPlaylistId
    ) {state, playing, activeId ->
        when {
            state == Player.STATE_BUFFERING && activeId == currPlaylistId -> PlayButtonState.Buffering
            playing && activeId == currPlaylistId -> PlayButtonState.PlayingThis
            else -> if (activeId == currPlaylistId) PlayButtonState.Idle else PlayButtonState.PlayingOther
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayButtonState.Idle)

    fun retrieveTrackIds(playlistId: String) {
        viewModelScope.launch {
            _playlistUiState.value = PlaylistUiState.Loading
            val firestorePlaylist = firestorePlaylistDao.getPlaylist(playlistId)
            if(firestorePlaylist == null) {
                _playlistUiState.value = PlaylistUiState.Error("Could not retrieve playlist")
                return@launch
            }
            val trackIds = firestorePlaylist.trackIds
            val deezerTracks = trackIds.map { trackId ->
                async {
                    deezerRepository.getTrackById(trackId)
                }
            }.awaitAll()
            val tracks = deezerTracks.map { it.toTrack() }
            _playlistUiState.value = PlaylistUiState.Success(firestorePlaylist, tracks)

            fetchStreamUrl(tracks).collect { fetchedTrack ->
                val currentTracks = (_playlistUiState.value as PlaylistUiState.Success).trackList.toMutableList()
                val position = currentTracks.indexOfFirst { it.id == fetchedTrack.id  }
                if(position != -1) {
                    currentTracks[position] = fetchedTrack
                    _playlistUiState.value = PlaylistUiState.Success(firestorePlaylist, currentTracks.toList())
                }
            }
        }
    }

    private fun fetchStreamUrl(tracks: List<Track>): Flow<Track> = channelFlow {
        val semaphore = Semaphore(5)
        tracks.map { track ->
            async {
                semaphore.withPermit {
                    val fetchedTrack = trackUrlResolver.resolve(track)
                    fetchedTrack?.let { send(it) }
                }
            }
        }.awaitAll()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            track.streamUrl?.let {
                playerController.playTracks(listOf(track), currPlaylistId)
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
            if(playingPlaylistId.value == currPlaylistId) {
                playerController.togglePlayPause()
            } else {
                playerController.clearQueue()
                playNextTracks()
            }
        }
        else {
            playerController.clearQueue()
            playNextTracks()
        }
    }

    private fun playNextTracks(startIndex: Int = 0) {
        val tracksToPlay = (_playlistUiState.value as PlaylistUiState.Success).trackList
        playerController.playPlaylist(tracksToPlay, currPlaylistId)
    }

//    private fun playNextTracks(startIndex: Int = 0) =
//        viewModelScope.launch(Dispatchers.Main) {
//            val tracksToPlay = (_playlistUiState.value as PlaylistUiState.Success).trackList
//                .subList(startIndex, startIndex + TRACKS_TO_PLAY)
//
//            /* For each of the next TRACKS_TO_PLAY tracks
//            Iff it has stream url, play
//            if not, try to fetch it one more time
//            If succeed play, otherwise skip
//            * */
//            tracksToPlay.forEach { track ->
//                if (track.streamUrl != null) {
//                    playerController.enqueueTracks(listOf(track), playlistId = currPlaylistId)
//                } else {
//                    Log.e("PlaylistViewModel", "No stream url found, retrying, for track ${track.title}")
//                    val fetchedTrack = trackUrlResolver.resolve(track)
//                    if(fetchedTrack != null && fetchedTrack.streamUrl != null) {
//                        _playlistUiState.update { state ->
//                            if (state !is PlaylistUiState.Success) return@update state
//                            state.copy(
//                                trackList = state.trackList.map { item ->
//                                    if(item.id == fetchedTrack.id) item.copy(streamUrl = fetchedTrack.streamUrl)
//                                    else item
//                                }
//                            )
//                        }
//                        playerController.enqueueTracks(listOf(fetchedTrack), playlistId = currPlaylistId)
//                    } else {
//                        Log.e("PlaylistViewModel", "Error trying to re-fetch url for track ${track.title}")
//                    }
//                }
//            }
//    }

    companion object {
        private const val TRACKS_TO_PLAY = 5
    }
}