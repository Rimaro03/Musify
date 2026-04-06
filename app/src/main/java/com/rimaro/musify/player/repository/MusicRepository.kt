package com.rimaro.musify.player.repository

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.rimaro.musify.player.service.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller get() = controllerFuture?.let {
        if (!it.isDone && !it.isCancelled) it.get() else null
    }

    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken)
            .buildAsync()
    }

    fun disconnect() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    fun play(tracks: List<MediaItem>, startIndex: Int = 0) {
        controller?.run {
            setMediaItems(tracks, startIndex, C.TIME_UNSET)
            prepare()
            play()
        }
    }

    fun pause() = controller?.pause()
    fun resume() = controller?.play()
    fun skipNext() = controller?.seekToNextMediaItem()
    fun skipPrev() = controller?.seekToPreviousMediaItem()
    fun seekTo(position: Long) = controller?.seekTo(position)

    fun getCurrentMediaItem(): MediaItem? = controller?.currentMediaItem
    fun isPlaying(): Boolean = controller?.isPlaying ?: false
}