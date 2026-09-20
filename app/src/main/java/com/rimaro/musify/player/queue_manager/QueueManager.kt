package com.rimaro.musify.player.queue_manager

import android.util.Log
import com.rimaro.musify.di.AppScope
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.repository.audio_url.AudioUrlRepository
import com.rimaro.musify.domain.repository.audio_url.ResolutionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map
import kotlin.math.min

@Singleton
class QueueManager @Inject constructor(
    @AppScope private val scope: CoroutineScope,
    private val audioUrlRepository: AudioUrlRepository
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
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = originalQueue.value
    )

    private val resolvedTracks: StateFlow<List<Long>> = audioUrlRepository.resolutionState.map { tracks ->
        tracks.filterValues { it is ResolutionState.Success }.keys.toList()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    private val pendingResolution: StateFlow<List<Long>> = audioUrlRepository.resolutionState.map { tracks ->
        tracks.filterValues { it is ResolutionState.Loading }.keys.toList()
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _tracksReady = Channel<ReadyBatch>(capacity = Channel.UNLIMITED)
    val tracksReady: ReceiveChannel<ReadyBatch> = _tracksReady

    private var windowStartTrackId: Long? = null
    private val windowStartIndex get() = activeQueue.indexOfFirst { it.id == windowStartTrackId }
    private var addedUpToId: Long? = null
    private val addedUpToIndex get() = activeQueue.indexOfFirst { it.id == addedUpToId }

    // to have only one window fetch at a time
    private var refillJob: Job? = null

    // to check for track fetched after a queue change, these tracks will be discarded by PlayerController
    @Volatile
    var generation = 0
        private set

    init {
        audioUrlRepository.resolutionState.value
    }

    // -------------- //
    // PUBLIC METHODS //
    // -------------- //

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

        // fetch the next window of tracks when withing the last 3 fetched tracks
        val offsetWithinWindow = currTrackPos - windowStartIndex
        if (offsetWithinWindow >= REFETCH_TRIGGER) {
            val newWindowsIdx = min(windowStartIndex + WINDOW_SIZE, activeQueue.size - 1)
            windowStartTrackId = activeQueue[newWindowsIdx].id
            advanceWindow()
        }
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled.value = enabled
        if(enabled && !originalQueue.value.isEmpty()) {
            shuffledQueue.value = originalQueue.value.shuffled()
        }

        windowStartTrackId = activeQueue.firstOrNull()?.id
        addedUpToId = null
    }


    // PRIVATE METHODS //

    private fun advanceWindow() {
        if(refillJob?.isActive == true) return
        if(activeQueue.isEmpty()) return

        val localGeneration = generation
        val toResolve = mutableListOf<Track>()
        val end = minOf(windowStartIndex + WINDOW_SIZE, activeQueue.size)
        for (i in windowStartIndex until end) {
            toResolve.add(activeQueue[i])
        }

        refillJob = scope.launch {
            toResolve.forEach { track ->
                coroutineScope {
                    resolve(track)
                    flushToPlayer(localGeneration)
                }
            }
        }
    }

    private suspend fun resolve(track: Track) {
        val url = audioUrlRepository.resolve(track)
        if(url != null) {
            track.streamUrl = url
        } else {
            Log.e("QueueManager", "Audio URL resolution failed for track ${track.id}")
        }
    }

    private suspend fun flushToPlayer(localGeneration: Int) {
        val toFlush = mutableListOf<Track>()
        var next = addedUpToIndex + 1
        var nextTrack: Track? = activeQueue[next]
        while(nextTrack != null && nextTrack.id in resolvedTracks.value) {
            toFlush.add(nextTrack)
            next++
            nextTrack = activeQueue.getOrNull(next)
        }
        if(toFlush.isEmpty()) {
            // check if track url retrieval failed
            if(nextTrack?.id !in pendingResolution.value) {
                addedUpToId = activeQueue[addedUpToIndex + 1].id
            }
            return
        }
        addedUpToId = toFlush.last().id

        _tracksReady.send(ReadyBatch(localGeneration,toFlush))
    }

    fun enqueue(newTrack: Track) {
        originalQueue.update { it + newTrack }
        shuffledQueue.update { it + newTrack }
    }

    fun playNext(currTrack: Track, newTrack: Track) {
        originalQueue.update { list ->
            val playingTrackIdx = list.indexOfFirst { it.id == currTrack.id }
            list.toMutableList().apply { add(playingTrackIdx + 1, newTrack) }
        }
        shuffledQueue.update { list ->
            val playingTrackIdx = list.indexOfFirst { it.id == currTrack.id }
            list.toMutableList().apply { add(playingTrackIdx + 1, newTrack) }
        }
    }

    fun move(from: Int, to: Int, currTrackId: Long) {
        // queue only show up next, need to convert from and to
        val base = activeQueue.indexOfFirst { it.id == currTrackId } + 1
        val updatedFrom = base + from
        val updatedTo = base + to
        Log.d("QueueManager", "$from - $to")
        if (updatedFrom == updatedTo || updatedFrom !in activeQueue.indices || updatedTo !in activeQueue.indices) return
        if(shuffleEnabled.value) {
            shuffledQueue.update {
                val list = it.toMutableList()
                list.add(updatedTo, list.removeAt(updatedFrom))
                list
            }
        } else {
            originalQueue.update {
                val list = it.toMutableList()
                list.add(updatedTo, list.removeAt(updatedFrom))
                list
            }
        }
//        if(shuffleEnabled.value) {
//            shuffledQueue.update {
//                newList
//            }
//        } else {
//            originalQueue.update {
//                newList
//            }
//        }
    }



    fun resetAddedUpToCount(currTrackId: String?) {
        if(currTrackId == null) return

        refillJob?.cancel()
        addedUpToId = currTrackId.toLong()
        windowStartTrackId = getNextTrack(currTrackId.toLong())?.id
        generation++
        advanceWindow()
    }

    private fun getNextTrack(currTrackId: Long) : Track? {
        val currTrackIdx = activeQueue.indexOfFirst { it.id == currTrackId }
        return if( currTrackIdx + 1 == activeQueue.size ) null
                else activeQueue[currTrackIdx + 1]
    }

    private fun reset() {
        originalQueue.value = emptyList()
        shuffledQueue.value = emptyList()
        windowStartTrackId = 0
        addedUpToId = null
        refillJob = null
    }

    companion object {
        const val WINDOW_SIZE = 5
        const val REFETCH_TRIGGER = 3
    }
}