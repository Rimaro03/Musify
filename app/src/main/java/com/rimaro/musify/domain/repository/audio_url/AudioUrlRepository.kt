package com.rimaro.musify.domain.repository.audio_url

import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.resolver.TrackUrlResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioUrlRepository @Inject constructor(
    private val trackUrlResolver: TrackUrlResolver
) {
    private val _resolutionState = MutableStateFlow<Map<Long, ResolutionState>>(emptyMap())
    val resolutionState: StateFlow<Map<Long, ResolutionState>> = _resolutionState.asStateFlow()

    /**
     * Resolve the audio url of the provided track
     * @param track instance of class Track to resolve
     * @return streamUrl, can be null if invalid
     * */
    suspend fun resolve(track: Track): String? {
        if(_resolutionState.value[track.id] is ResolutionState.Success)
            return (_resolutionState.value[track.id] as ResolutionState.Success).streamUrl

        _resolutionState.update { it + (track.id to ResolutionState.Loading) }
        val (streamUrl, _) = trackUrlResolver.resolve(track)
        if (streamUrl == null) {
            _resolutionState.update { it + (track.id to ResolutionState.Error) }
        } else {
            _resolutionState.update { it + (track.id to ResolutionState.Success(streamUrl)) }
        }
        return streamUrl
    }
}