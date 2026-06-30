package com.rimaro.musify.player.controller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.rimaro.musify.di.AppScope
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.queue_manager.QueueManager
import com.rimaro.musify.player.service.MusicService
import com.rimaro.musify.util.MediaItemMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueManager: QueueManager,
    @AppScope private val coroutineScope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller get() = if (controllerFuture?.isDone == true && controllerFuture?.isCancelled == false) {
        controllerFuture?.get()
    } else null

    private val _playerState = MutableStateFlow<Int>(Player.STATE_IDLE)
    val playerState: StateFlow<Int> = _playerState

    private val _isPlaying = MutableStateFlow<Boolean>(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _shuffleEnabled = MutableStateFlow<Boolean>(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack

    private val _playingPlaylistId = MutableStateFlow<String?>("1")
    val playingPlaylistId: StateFlow<String?> = _playingPlaylistId

    init {
        connect()
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playerState.value = playbackState
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                _currentTrack.value =  MediaItemMapper.toTrack(it)
                queueManager.onCurrentTrackChange(mediaItem.mediaId.toLong())
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleEnabled.value = shuffleModeEnabled
            queueManager.setShuffleEnabled(shuffleModeEnabled)
        }
    }

    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .also { future ->
                future.addListener({
                    val controller = future.get()
                    controller.playWhenReady = true
                    controller.addListener(playerListener)
                    _currentTrack.value = controller.currentMediaItem?.let {
                        MediaItemMapper.toTrack(it)
                    }
                }, MoreExecutors.directExecutor())
            }
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    fun playTracks(tracks: List<Track>, fromPlaylistId: String?) {
        controller?.run {
            _playingPlaylistId.value = fromPlaylistId
            setMediaItems(MediaItemMapper.fromTracks(tracks))
            if(controller?.playbackState == Player.STATE_IDLE) prepare()
            play()
        }
    }

    fun pause() = controller?.pause()
    fun resume() = controller?.play()
    fun togglePlayPause() = controller?.let {
        if(it.isPlaying) it.pause()
        else it.play()
    }

    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrev() = controller?.seekToPreviousMediaItem()
    fun seekTo(position: Long) = controller?.seekTo(position)

    fun enqueueTracks(tracks: List<Track>, position: Int? = null, playlistId: String?) {
        controller?.run {
            _playingPlaylistId.value = playlistId
            addMediaItems(
                position ?: mediaItemCount ,
                MediaItemMapper.fromTracks(tracks)
            )
            if(controller?.playbackState == Player.STATE_IDLE) prepare()
        }
    }

    fun toggleShuffle() {
        controller?.shuffleModeEnabled = controller?.shuffleModeEnabled?.not() ?: false
    }

    fun getCurrentMediaItem(): MediaItem? = controller?.currentMediaItem
    fun isPlaying(): Boolean = controller?.isPlaying ?: false
    fun clearQueue() = controller?.clearMediaItems()

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        _currentTrack.value = null
        _isPlaying.value = false
        _playingPlaylistId.value = null
    }

    fun playPlaylist(tracks: List<Track>, playlistId: String) {
        queueManager.loadQueue(tracks, shuffleEnabled.value)

        coroutineScope.launch(Dispatchers.Main) {
            queueManager.tracksReady.collect { tracks ->
                tracks.forEach { track ->
                    enqueueTracks(listOf(track), playlistId = playlistId)
                }
            }
        }

        coroutineScope.launch {
            queueManager.queue.collect { queue ->
                val queueString = queue.joinToString("\n") { it.title }
            }
        }
    }

}