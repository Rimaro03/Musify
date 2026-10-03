package com.rimaro.musify.ui.library.playlists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.data.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.ui.library.LibraryUiState
import com.rimaro.musify.util.playlist_import.PlaylistImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    private val deezerRepository: DeezerRepository,
    private val playlistImporter: PlaylistImporter,

) : ViewModel() {
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying

    val libraryUiState: StateFlow<PlaylistsUiState> = firestorePlaylistRepo
        .observeUserPlaylists()
        .map { userPlaylists ->
            PlaylistsUiState.Success(userPlaylists)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            PlaylistsUiState.Loading
        )

    fun createThumbnail(playlistId: String) = viewModelScope.launch {
        // get covers
        val firestorePlaylist = firestorePlaylistRepo.getPlaylist(playlistId)
        if(firestorePlaylist == null) {
            Log.e("LibraryViewmodel", "Could not fetch firestore playlist during thumbnail creation")
            return@launch
        }

        val deezerTracks = firestorePlaylist.tracks.take(4).map { trackId ->
            async {
                deezerRepository.getTrackById(trackId)
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
    }
}