package com.rimaro.musify.domain.repository

import com.rimaro.musify.data.remote.DeezerApi
import com.rimaro.musify.domain.model.DeezerArtist
import com.rimaro.musify.domain.model.DeezerAutocompleteRes
import com.rimaro.musify.domain.model.DeezerGenre
import com.rimaro.musify.domain.model.DeezerSearchRes
import javax.inject.Inject

class DeezerRepository @Inject constructor(
    private val deezerApi: DeezerApi
) {
    suspend fun search(query: String) : DeezerSearchRes {
        return deezerApi.search(query)
    }

    suspend fun autocomplete(query: String) : DeezerAutocompleteRes {
        return deezerApi.autocomplete(query)
    }

    suspend fun getTopArtists(): List<DeezerArtist> {
        return deezerApi.topArtists().data
    }

    suspend fun getGenres(): List<DeezerGenre> {
        return deezerApi.genres().data
    }
}