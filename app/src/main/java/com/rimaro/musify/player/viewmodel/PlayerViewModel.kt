package com.rimaro.musify.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import com.rimaro.musify.domain.model.PlayerState
import com.rimaro.musify.player.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        repository.connect()
    }

    fun play(tracks: List<MediaItem>, startIndex: Int = 0) {
        repository.play(tracks, startIndex)
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

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}