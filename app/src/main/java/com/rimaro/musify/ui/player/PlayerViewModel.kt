package com.rimaro.musify.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.ui.common.PlayButtonState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val playerController: PlayerController
) : AndroidViewModel(application) {

    val playerState: StateFlow<Int> = playerController.playerState
    val isPlaying: StateFlow<Boolean> = playerController.isPlaying
    val currentTrack: StateFlow<Track?> = playerController.currentTrack
    val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
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

    override fun onCleared() {
        playerController.disconnect()
        super.onCleared()
    }

}