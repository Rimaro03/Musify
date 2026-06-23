package com.rimaro.musify.ui

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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlaybackViewmodel @Inject constructor(
    application: Application,
    private val trackUrlResolver: TrackUrlResolver,
    private val firestorePlaylistDao: FirestorePlaylistDao,
    private val deezerRepository: DeezerRepository,
    private val playerController: PlayerController
) : AndroidViewModel(application) {
    private var _fetchingTracks = MutableStateFlow(false)
    val fetchingTracks: StateFlow<Boolean> = _fetchingTracks

    var playingPlaylistId: String? = null

    fun playPlaylist(playlistId: String) {
        playingPlaylistId = playlistId
        _fetchingTracks.value = true
        viewModelScope.launch {
            // get playlist from Firestore
            val playlist = firestorePlaylistDao.getPlaylist(playlistId) ?: return@launch

            // get deezer tracks
            val semaphore = Semaphore(5)
            val deezerTracks = playlist.trackIds.map { trackId ->
                async {
                    semaphore.withPermit {
                        deezerRepository.getTrackById(trackId)
                    }
                }
            }.awaitAll()

            // get audio url from deezer track
            val chunkedDeezerTracks = deezerTracks.chunked(5)
            playerController.clearQueue()
            chunkedDeezerTracks.forEachIndexed  { index, chunk ->
                val tracks = chunk.map { deezerTrack ->
                    async {
                        trackUrlResolver.resolve(deezerTrack.toTrack())
                    }
                }.awaitAll().mapNotNull { it }
                _fetchingTracks.value = false
                playerController.enqueueTracks(tracks, playlistId = playlistId)
            }
        }
    }
}