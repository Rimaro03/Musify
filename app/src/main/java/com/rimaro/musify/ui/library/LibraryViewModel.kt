package com.rimaro.musify.ui.library

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.google.firebase.auth.FirebaseAuth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.util.ImportPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val importPlaylist: ImportPlaylist,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val playerController: PlayerController,
    private val deezerRepository: DeezerRepository
) : AndroidViewModel(application) {
    val importState = MutableStateFlow<ImportResult?>(null)

    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId

    private val _libraryUiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val libraryUiState: StateFlow<LibraryUiState> = _libraryUiState

    init {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        getUserPlaylists(userId)
    }

    // PLAYLIST IMPORTING METHODS //

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            importPlaylist.importFromCsv(uri)
                .collect { result ->
                    importState.value = result
                }
        }
    }


    // PLAYLIST PLAY METHODS //

    private fun getUserPlaylists(userId: String?) {
        if(userId == null) {
            _libraryUiState.value = LibraryUiState.Error("User is null")
            return
        }
        viewModelScope.launch {
            _libraryUiState.value = LibraryUiState.Loading
            val playlists = firestorePlaylistDao.getUserPlaylists(userId)
            _libraryUiState.value = LibraryUiState.Success(playlists)
        }
    }

    fun togglePlayButton(playlistId: String) {
        if(playerState.value == Player.STATE_BUFFERING) return

        if(playerState.value == Player.STATE_READY) {
            if(playingPlaylistId.value == playlistId) {
                playerController.togglePlayPause()
            } else {
                playerController.clearQueue()
                playPlaylist(playlistId)
            }
        }
        else {
            playerController.clearQueue()
            playPlaylist(playlistId)
        }
    }

    private fun playPlaylist(playlistId: String) {
        viewModelScope.launch {
            val firestorePlaylist = firestorePlaylistDao.getPlaylist(playlistId)
            if (firestorePlaylist == null) {
                Log.e("LibraryViewmodel", "Could not retrieve playlist")
                return@launch
            }
            val trackIds = firestorePlaylist.trackIds
            val deezerTracks = trackIds.map { trackId ->
                async {
                    deezerRepository.getTrackById(trackId)
                }
            }.awaitAll()
            val tracks = deezerTracks.map { it.toTrack() }

            playerController.playPlaylist(tracks, playlistId)
        }
    }
}
