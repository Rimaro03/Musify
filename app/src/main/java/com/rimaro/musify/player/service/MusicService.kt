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

class MusicService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

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
            .setCallback(MediaSessionCallback(likeTrackCommand, shuffleCommand))
            .setMediaButtonPreferences(ImmutableList.of(shuffleButton, likeTrackButton))
            .build()
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

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private class MediaSessionCallback(
        private val likeCmd: SessionCommand,
        private val shuffleCmd: SessionCommand
    ) : MediaSession.Callback {
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
                .add(shuffleCmd)
                .add(likeCmd)
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
            return when(customCommand.customAction) {
                ACTION_LIKE_TRACK -> {
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                ACTION_SHUFFLE -> {
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_LIKE_TRACK = "ACTION_LIKE_TRACK"
        const val ACTION_SHUFFLE = "ACTION_SHUFFLE"
    }
}
