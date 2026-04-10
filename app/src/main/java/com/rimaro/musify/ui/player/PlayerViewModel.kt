package com.rimaro.musify.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val playerController: PlayerController
) : AndroidViewModel(application) {

    val playerState: StateFlow<Int> = playerController.playerState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Player.STATE_IDLE)

    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentTrack: StateFlow<Track?> = playerController.currentTrack
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        playerController.connect()
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

    fun enqueueTracks(tracks: List<Track>, position: Int? = null) =
        playerController.enqueueTracks(tracks, position)

    fun toggleShuffle() = playerController.toggleShuffle()

    override fun onCleared() {
        playerController.disconnect()
        super.onCleared()
    }

}