package com.rimaro.musify.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.domain.model.FirestorePlaylist
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toFirestoreTrack
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.ui.common.PlayButtonState
import com.rimaro.musify.ui.common.model.TrackUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val playerController: PlayerController,
    private val firestoreLikedTracksRepo: FirestoreLikedTracksRepo,
    private val firestorePlaylistRepo: FirestorePlaylistRepo,
    private val queueManager: QueueManager
) : AndroidViewModel(application) {
    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentTrack: StateFlow<Track?> = playerController.currentTrack
    val queue: StateFlow<List<TrackUiModel>> = queueManager.queue
        .map { tracks ->
            tracks.map { TrackUiModel(it) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    val isLiked: StateFlow<Boolean> = combine(currentTrack, firestoreLikedTracksRepo.likedTracks)
    { currTrack, currLikedTracks ->
        if(currTrack != null) currLikedTracks.any { it.trackId == currTrack.id }
        else false
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
    val playingPlaylist: StateFlow<FirestorePlaylist?> = playingPlaylistId
        .map { playlistId ->
            playlistId?.let {
                firestorePlaylistRepo.getPlaylist(it)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val shuffleEnabled: StateFlow<Boolean> = playerController.shuffleEnabled
    val repeatMode: StateFlow<Int> = playerController.repeatMode
    val trackCurrPos: Long
        get() = playerController.currPosition
    val trackDuration: Long
        get() = playerController.duration
    val controllerReady = playerController.controllerReady

    val playButtonState: StateFlow<PlayButtonState> = combine(
        playerState, isPlaying, playingPlaylistId
    ) { state, playing, _ ->
        when {
            state == Player.STATE_BUFFERING -> PlayButtonState.Buffering
            playing -> PlayButtonState.PlayingThis
            else -> PlayButtonState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayButtonState.Idle)

    init {
        playerController.connect()
    }

    fun playTrack(track: Track) {
        viewModelScope.launch {
            track.streamUrl?.let {
                playerController.playTracks(listOf(track), playerController.playingPlaylistId.value)
            }
        }
    }

    fun pause() {
        playerController.pause()
    }
    fun resume() {
        playerController.resume()
    }
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrev()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleShuffle() = playerController.toggleShuffle()
    fun toggleRepeatMode() = playerController.toggleRepeatMode()

    fun addListener(listener: Player.Listener) = playerController.addListener(listener)
    fun removeListener(listener: Player.Listener) = playerController.removeListener(listener)

    fun toggleLike(track: Track) {
        if(isLiked.value) firestoreLikedTracksRepo.removeTrack(track.id)
        else firestoreLikedTracksRepo.addTrack(track.toFirestoreTrack())
    }

    override fun onCleared() {
        playerController.disconnect()
        super.onCleared()
    }

}