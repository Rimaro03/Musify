package com.rimaro.musify.player.queue_manager

import android.util.Log
import com.rimaro.musify.di.AppScope
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.resolver.TrackUrlResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor(
    @AppScope private val coroutineScope: CoroutineScope,
    private val trackUrlResolver: TrackUrlResolver,
) {
    private val originalQueue = MutableStateFlow<List<Track>>(emptyList())
    private val shuffledQueue = MutableStateFlow<List<Track>>(emptyList())
    private val shuffleEnabled = MutableStateFlow(false)

    private val activeQueue
        get() = if(shuffleEnabled.value) shuffledQueue.value else originalQueue.value

    val queue: StateFlow<List<Track>> = combine(
        originalQueue, shuffledQueue, shuffleEnabled
    ) { original, shuffled, enabled ->
        if (enabled) shuffled else original
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = originalQueue.value
    )

    private val resolvedTracks = mutableListOf<Track>()
    private val pendingResolution = mutableSetOf<Long>()

    private val _tracksReady = MutableSharedFlow<List<Track>>(extraBufferCapacity = 64)
    val tracksReady: SharedFlow<List<Track>> = _tracksReady.asSharedFlow()

    private var windowStartTrackId: Long? = null
    private val windowStartIndex get() = activeQueue.indexOfFirst { it.id == windowStartTrackId }
    private var addedUpToId: Long? = null
    private val addedUpToIndex get() = activeQueue.indexOfFirst { it.id == addedUpToId }

    companion object {
        const val WINDOW_SIZE = 5
        const val REFETCH_TRIGGER = 3
    }

    // PUBLIC METHODS //

    fun loadQueue(tracks: List<Track>, shuffle: Boolean) {
        reset()
        val shuffledTracks = tracks.shuffled()
        val queueToUse = if(shuffle) shuffledTracks else tracks

        originalQueue.value = tracks
        shuffledQueue.value = shuffledTracks
        shuffleEnabled.value = shuffle
        windowStartTrackId = queueToUse.first().id
        addedUpToId = null

        advanceWindow()
    }

    fun onCurrentTrackChange(trackId: Long) {
        val currTrackPos = activeQueue.indexOfFirst { it.id == trackId }
        // remove currently playing track from the queue
        originalQueue.value = originalQueue.value.filter { it.id != trackId }
        shuffledQueue.value = shuffledQueue.value.filter { it.id != trackId }

        if(currTrackPos == -1) {
            Log.e("QueueManager", "Could not fetch the current track position " +
                    "in the active queue\n TrackID: $trackId")
            return
        }
        if(windowStartIndex == -1) {
            Log.e("QueueManager", "Could not fetch the window first track position " +
                    "in the active queue\n TrackID: $trackId")
            return
        }

        val offsetWithinWindow = currTrackPos - windowStartIndex
        if (offsetWithinWindow >= REFETCH_TRIGGER) {
            windowStartTrackId = trackId
            advanceWindow()
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled.value = enabled
        if(enabled) {
            shuffledQueue.value = originalQueue.value.shuffled()
        }

        windowStartTrackId = activeQueue.first().id
        addedUpToId = null
    }


    // PRIVATE METHODS //

    private fun advanceWindow() {
        val end = minOf(windowStartIndex + WINDOW_SIZE, activeQueue.size)
        for (i in windowStartIndex until end) {
            val track = activeQueue[i]
            if (track !in resolvedTracks && track.id !in pendingResolution) {
                resolve(track)
            }
        }
    }

    private fun resolve(track: Track) {
        pendingResolution.add(track.id)
        coroutineScope.launch {
            val url = trackUrlResolver.resolve(track)?.streamUrl
            pendingResolution.remove(track.id)
            if(url != null) {
                track.streamUrl = url
                resolvedTracks.add(track)
                flushToPlayer()
            } else {
                Log.e("QueueManager", "Audio URL resolution failed for track ${track.id}")
            }
        }
    }

    private fun flushToPlayer() {
        val toFlush = mutableListOf<Track>()
        var next = addedUpToIndex + 1
        var nextTrack = activeQueue[next]
        while(nextTrack in resolvedTracks) {
            toFlush.add(nextTrack)
            next++
            nextTrack = activeQueue[next]
        }
        if(toFlush.isEmpty()) {
            // check if track url retrieval failed
            if(nextTrack.id !in pendingResolution) {
                addedUpToId = activeQueue[addedUpToIndex + 1].id
            }
            return
        }
        addedUpToId = toFlush.last().id

        coroutineScope.launch {
            _tracksReady.emit(toFlush)
        }
    }

    private fun reset() {
        originalQueue.value = emptyList()
        shuffledQueue.value = emptyList()
        resolvedTracks.clear()
        pendingResolution.clear()
        shuffleEnabled.value = false
        windowStartTrackId = null
        addedUpToId = null
    }
}