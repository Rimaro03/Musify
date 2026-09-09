package com.rimaro.musify.ui.library

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.google.firebase.auth.FirebaseAuth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.util.playlist_import.PlaylistImporter
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
    private val playlistImporter: PlaylistImporter,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    private val playerController: PlayerController,
    private val deezerRepository: DeezerRepository,
    private val queueManager: QueueManager
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
            playlistImporter.importFromCsv(uri)
                .collect { result ->
                    importState.value = result
                }
        }
    }

    fun createThumbnail(playlistId: String) {
        viewModelScope.launch {
            // get covers
            val firestorePlaylist = firestorePlaylistRepo.getPlaylist(playlistId)
            if(firestorePlaylist == null) {
                Log.e("LibraryViewmodel", "Could not fetch firestore playlist during thumbnail creation")
                return@launch
            }

            val deezerTracks = firestorePlaylist.tracks.take(4).map { track ->
                async {
                    deezerRepository.getTrackById(track.trackId)
                }
            }.awaitAll()

            val covers = if(deezerTracks.size < 4) {
                listOf(deezerTracks.first().album?.coverXl ?: return@launch)
            } else {
                deezerTracks.map { it.album?.coverXl ?: return@launch}
            }

            // create thumbnail
            val newThumbnailPath = playlistImporter.createPlaylistThumbnail(covers, playlistId)
                ?: return@launch

            // update playlist with thumbnail
            firestorePlaylistRepo.updatePlaylistThumbnail(playlistId, newThumbnailPath)

            // update playlist cover
            val currentList = _libraryUiState.value as? LibraryUiState.Success ?: return@launch

            val updatedList = currentList.res.map { playlist ->
                if(playlist.id == playlistId) {
                    playlist.copy(thumbnailPath = newThumbnailPath)
                } else {
                    playlist
                }
            }

            _libraryUiState.value = LibraryUiState.Success(updatedList)
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
            val playlists = firestorePlaylistRepo.getUserPlaylists(userId)
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
            val firestorePlaylist = firestorePlaylistRepo.getPlaylist(playlistId)
            if (firestorePlaylist == null) {
                Log.e("LibraryViewmodel", "Could not retrieve playlist")
                return@launch
            }
            val trackIds = firestorePlaylist.tracks
            val deezerTracks = trackIds.map { track ->
                async {
                    deezerRepository.getTrackById(track.trackId)
                }
            }.awaitAll()
            val tracks = deezerTracks.map { it.toTrack() }

            queueManager.loadQueue(tracks, playerController.shuffleEnabled.value)
        }
    }
}
