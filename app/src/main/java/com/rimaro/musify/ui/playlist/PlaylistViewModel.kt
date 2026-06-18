package com.rimaro.musify.ui.playlist

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.controller.PreviewPlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    application: Application,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val deezerRepository: DeezerRepository,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController,
    private val previewPlayerController: PreviewPlayerController
) : AndroidViewModel(application) {
    private val _playlistUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val playlistUiState = _playlistUiState.asStateFlow()

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId

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

    fun playTrack(track: Track, playlistId: String) {
        viewModelScope.launch {
            track.streamUrl?.let {
                playerController.playTracks(listOf(track), playlistId)
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

    fun togglePlayButton(playlistId: String) {
        // divide case in playing, paused, not playing, change behaviour accordingly
        // also consider if change of behaviour should be together with change of icon (not as it is now)
        playNextTracks(playlistId = playlistId)
    }

    private fun playNextTracks(startIndex: Int = 0, playlistId: String) =
        viewModelScope.launch(Dispatchers.Main) {
            val tracksToPlay = (_playlistUiState.value as PlaylistUiState.Success).trackList
                .subList(startIndex, startIndex + TRACKS_TO_PLAY)

            /* For each of the next TRACKS_TO_PLAY tracks
            Iff it has stream url, play
            if not, try to fetch it one more time
            If succeed play, otherwise skip
            * */
            tracksToPlay.forEach { track ->
                if (track.streamUrl != null) {
                    playerController.enqueueTracks(listOf(track), playlistId=playlistId)
                } else {
                    Log.e("PlaylistViewModel", "No stream url found, retrying, for track ${track.title}")
                    val fetchedTrack = trackUrlResolver.resolve(track)
                    if(fetchedTrack != null && fetchedTrack.streamUrl != null) {
                        _playlistUiState.update { state ->
                            if (state !is PlaylistUiState.Success) return@update state
                            state.copy(
                                trackList = state.trackList.map { item ->
                                    if(item.id == fetchedTrack.id) item.copy(streamUrl = fetchedTrack.streamUrl)
                                    else item
                                }
                            )
                        }
                        playerController.enqueueTracks(listOf(fetchedTrack), playlistId = playlistId)
                    } else {
                        Log.e("PlaylistViewModel", "Error trying to re-fetch url for track ${track.title}")
                    }
                }
            }
    }

    companion object {
        private const val TRACKS_TO_PLAY = 5
    }
}