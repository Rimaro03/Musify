package com.rimaro.musify.ui.player

import androidx.lifecycle.ViewModel
import com.rimaro.musify.domain.model.PlayerState
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.controller.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerController
) : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow<Boolean>(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    init {
        repository.connect()
    }

    fun play(tracks: List<Track>) {
        repository.playTracks(tracks)
        _playerState.value = PlayerState.Playing
    }

    fun pause() {
        repository.pause()
        _playerState.value = PlayerState.Paused
    }
    fun resume() {
        repository.resume()
        _playerState.value = PlayerState.Playing
    }
    fun skipNext() = repository.skipNext()
    fun skipPrevious() = repository.skipPrev()
    fun seekTo(positionMs: Long) = repository.seekTo(positionMs)

    fun enqueueTracks(tracks: List<Track>, position: Int? = null) =
        repository.enqueueTracks(tracks, position)

    fun toggleShuffle() = repository.toggleShuffle()

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }

}