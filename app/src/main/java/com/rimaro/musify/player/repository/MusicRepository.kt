package com.rimaro.musify.player.repository

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.player.service.MusicService
import com.rimaro.musify.util.MediaItemMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller get() = if (controllerFuture?.isDone == true && controllerFuture?.isCancelled == false) {
        controllerFuture?.get()
    } else null

    init {
        connect()
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
                    future.get().playWhenReady = true
                }, MoreExecutors.directExecutor())
            }
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    fun playTracks(tracks: List<Track>) {
        controller?.run {
            setMediaItems(MediaItemMapper.fromTracks(tracks))
            if(controller?.playbackState == Player.STATE_IDLE) prepare()
        }
    }

    fun pause() = controller?.pause()
    fun resume() = controller?.play()
    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrev() = controller?.seekToPreviousMediaItem()
    fun seekTo(position: Long) = controller?.seekTo(position)

    fun enqueueTracks(tracks: List<Track>, position: Int?) {
        controller?.run {
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
}