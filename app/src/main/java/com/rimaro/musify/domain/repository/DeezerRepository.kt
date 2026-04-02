package com.rimaro.musify.domain.repository

import com.rimaro.musify.data.remote.DeezerApi
import com.rimaro.musify.domain.dto.DeezerArtist
import com.rimaro.musify.domain.dto.DeezerAutocompleteRes
import com.rimaro.musify.domain.dto.DeezerGenre
import com.rimaro.musify.domain.dto.DeezerSearchRes
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