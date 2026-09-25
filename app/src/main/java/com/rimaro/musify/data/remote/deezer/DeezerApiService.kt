package com.rimaro.musify.data.remote.deezer

import com.rimaro.musify.data.remote.deezer.dto.DeezerAutocompleteRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerChartArtistRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerGenreRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerSearchRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerSearchTrackRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerTrack
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface DeezerApiService {
    @GET("search/")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 25,
        @Query("index") offset: Int = 0,
    ): DeezerSearchRes

    @GET("search/track/")
    suspend fun searchTrack(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("index") offset: Int,
    ): DeezerSearchTrackRes

    @GET("search/autocomplete/")
    suspend fun autocomplete(
        @Query("q") query: String
    ): DeezerAutocompleteRes

    @GET("/chart/0/artists/")
    suspend fun topArtists() : DeezerChartArtistRes

    @GET("/genre/")
    suspend fun genres() : DeezerGenreRes

    @GET("/track/{track_id}")
    suspend fun track(
        @Path("track_id") trackId: String
    ) : DeezerTrack
}