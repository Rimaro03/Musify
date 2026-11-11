package com.rimaro.musify.data.remote.deezer.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeezerAutocompleteRes (
    val tracks: ListRes<DeezerTrack>,
    val albums: ListRes<DeezerAlbum>,
    val artists: ListRes<DeezerArtist>,
)