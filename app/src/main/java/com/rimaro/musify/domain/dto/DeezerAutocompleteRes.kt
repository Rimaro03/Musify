package com.rimaro.musify.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeezerAutocompleteRes (
    val tracks: DeezerAutocompleteList<DeezerTrack>,
    val albums: DeezerAutocompleteList<DeezerAlbum>,
    val artists: DeezerAutocompleteList<DeezerArtist>,
)

@Serializable
data class DeezerAutocompleteList<T>(
    val data: List<T>
)