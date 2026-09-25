package com.rimaro.musify.data.repository.audio_url

sealed class ResolutionState {
    object Loading : ResolutionState()
    data class Success(val streamUrl: String) : ResolutionState()
    object Error : ResolutionState()
}