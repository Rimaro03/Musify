package com.rimaro.musify.ui.playlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    application: Application,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val deezerRepository: DeezerRepository,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController
) : AndroidViewModel(application) {
    private val _playlistUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Idle)
    val playlistUiState = _playlistUiState.asStateFlow()

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
            _playlistUiState.value = PlaylistUiState.Success(tracks)

            fetchStreamUrl(tracks).collect { fetchedTrack ->
                val currentTracks = (_playlistUiState.value as PlaylistUiState.Success).trackList.toMutableList()
                val position = currentTracks.indexOfFirst { it.id == fetchedTrack.id  }
                if(position != -1) {
                    currentTracks[position] = fetchedTrack
                    _playlistUiState.value = PlaylistUiState.Success(currentTracks.toList())
                }
            }
        }
    }

    private fun fetchStreamUrl(tracks: List<Track>): Flow<Track> = channelFlow {
        tracks.map { track ->
            async {
                val fetchedTrack = trackUrlResolver.resolve(track)
                fetchedTrack?.let { send(it) }
            }
        }.awaitAll()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            track.streamUrl?.let {
                playerController.playTracks(listOf(track))
            }
        }
    }

    fun playPlaylist() {}
}