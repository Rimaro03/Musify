package com.rimaro.musify.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    fun playPlaylist(playlistId: String) {
        /*
        1. fetch track ids from firestore
        2. parallel api calls to deezer to fetch track data
         */
        viewModelScope.launch {
            val playlist = firestorePlaylistDao.getPlaylist(playlistId) ?: return@launch
            Log.d("play", "firestore ${playlist}")
            val semaphore = Semaphore(5)
            val deezerTracks = playlist.trackIds.map { trackId ->
                async {
                    semaphore.withPermit {
                        deezerRepository.getTrackById(trackId)
                    }
                }
            }.awaitAll()
            Log.d("play", "Deezer track: $deezerTracks")
            val tracks = deezerTracks.map { deezerTrack ->
                async {
                    trackUrlResolver.resolve(deezerTrack.toTrack())
                }
            }.awaitAll().mapNotNull { it }
            Log.d("play", "Tracks: $tracks")
            playerController.playTracks(tracks)
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
}