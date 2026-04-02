package com.rimaro.musify.data.remote

import com.rimaro.musify.domain.dto.DeezerArtist
import com.rimaro.musify.domain.dto.DeezerAutocompleteRes
import com.rimaro.musify.domain.dto.DeezerChartArtistRes
import com.rimaro.musify.domain.dto.DeezerGenre
import com.rimaro.musify.domain.dto.DeezerGenreRes
import com.rimaro.musify.domain.dto.DeezerSearchRes
import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApi {
    @GET("search/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25,
        @Query("index") offset: Int = 0,
    ): DeezerSearchRes

    @GET("search/autocomplete/")
    suspend fun autocomplete(
        @Query("q") query: String
    ): DeezerAutocompleteRes

    @GET("/chart/0/artists/")
    suspend fun topArtists() : DeezerChartArtistRes

    @GET("/genre/")
    suspend fun genres() : DeezerGenreRes
}