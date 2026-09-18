package com.rimaro.musify.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rimaro.musify.R
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.util.MediaItemMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    @Inject lateinit var likedTracksRepo: FirestoreLikedTracksRepo
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val likeTrackCommand = SessionCommand(ACTION_LIKE_TRACK, Bundle.EMPTY)
    private val likeTrackButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("Like track")
        .setCustomIconResId(androidx.media3.session.R.drawable.media3_icon_heart_unfilled)
        .setSessionCommand(likeTrackCommand)
        .build()

    private val shuffleCommand = SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY)
    private val shuffleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
        .setDisplayName("Like track")
        .setCustomIconResId(androidx.media3.session.R.drawable.media3_icon_shuffle_off)
        .setSessionCommand(likeTrackCommand)
        .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            //.setMediaButtonPreferences(ImmutableList.of(shuffleButton, likeTrackButton))
            .build()

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshButtons()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                refreshButtons()
            }
        })

        scope.launch { likedTracksRepo.likedTracks.collect { refreshButtons() } }

        refreshButtons()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        try {
            val notification = buildPlaceholderNotification()

            if (notification == null) {
                Log.e("MusicService", "Notification is null!")
                stopSelf()
                return START_NOT_STICKY
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                // For Android 13 and below - STILL NEED THIS!
                startForeground(NOTIFICATION_ID, notification)
            }

        } catch (e: Exception) {
            Log.e("MusicService", "Error starting foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun buildPlaceholderNotification(): Notification {
        val channelId = "media_playback_channel"
        val channel = NotificationChannel(
            channelId,
            "Media Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.music_note_2_24px)
            .setContentTitle("Music Player")
            .setSilent(true)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            val resolvedItems = mediaItems.map { item ->
                item.buildUpon()
                    .setUri(item.requestMetadata.mediaUri)
                    .build()
            }
            return Futures.immediateFuture(resolvedItems)
        }

        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(shuffleCommand)
                .add(likeTrackCommand)
                .build()


            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when(customCommand.customAction) {
                ACTION_LIKE_TRACK -> {
                    session.player.currentMediaItem?.let {
                        likedTracksRepo.toggleLike(MediaItemMapper.toTrack(it))
                    }
                }

                ACTION_SHUFFLE -> {
                    session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                }

            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    @OptIn(UnstableApi::class)
    private fun refreshButtons() {
        val session = mediaSession ?: return
        val player = session.player
        val liked = player.currentMediaItem?.mediaId?.let { likedTracksRepo.isLiked(it.toLong()) } ?: false

        val likeBtn = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Like track")
            .setCustomIconResId(
                if(liked) androidx.media3.session.R.drawable.media3_icon_heart_filled
                else androidx.media3.session.R.drawable.media3_icon_heart_unfilled
            )
            .setSessionCommand(likeTrackCommand)
            .build()

        val shuffleBtn = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setDisplayName("Like track")
            .setCustomIconResId(
                if(player.shuffleModeEnabled) androidx.media3.session.R.drawable.media3_icon_shuffle_on
                else androidx.media3.session.R.drawable.media3_icon_shuffle_off
            )
            .setSessionCommand(shuffleCommand)
            .build()

        session.setMediaButtonPreferences(listOf(shuffleBtn, likeBtn))
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_LIKE_TRACK = "ACTION_LIKE_TRACK"
        const val ACTION_SHUFFLE = "ACTION_SHUFFLE"
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
