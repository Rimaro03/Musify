package com.rimaro.musify.player.controller

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class PreviewPlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentTrackId: String? = null
    private var progressJob: Job? = null

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when(playbackState) {
                    Player.STATE_ENDED -> {
                        _progress.update { 0f }
                        currentTrackId = null
                        exoPlayer.seekTo(0)
                        stopProgressLoop()
                    }
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startProgressLoop() else stopProgressLoop()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                stopProgressLoop()
            }
        })
    }

    fun playPreview(trackId: String, previewUrl: String) {
        if(currentTrackId == trackId) {
            stop()
            return
        }

        currentTrackId = trackId
        _progress.update { 0f }

        exoPlayer.setMediaItem(MediaItem.fromUri(previewUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun startProgressLoop() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (true) {
                val duration = exoPlayer.duration
                if (duration > 0) {
                    val fraction = exoPlayer.currentPosition.toFloat() / duration.toFloat()
                    _progress.update { fraction.coerceIn(0f, 1f) }
                }
                delay(50L.milliseconds)
            }
        }
    }

    fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentTrackId = null
        _progress.update { 0f }
        stopProgressLoop()
    }

    fun release() {
        exoPlayer.release()
    }
}