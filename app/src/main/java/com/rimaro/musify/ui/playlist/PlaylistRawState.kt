package com.rimaro.musify.ui.playlist

import com.rimaro.musify.domain.model.FirestorePlaylist
import com.rimaro.musify.domain.model.Track

sealed class PlaylistRawState {
    object Idle : PlaylistRawState()
    object Loading : PlaylistRawState()
    data class Success(
        val playlist: FirestorePlaylist,
        val trackList: List<Track>
    ) : PlaylistRawState()
    data class Error(val message: String) : PlaylistRawState()
}