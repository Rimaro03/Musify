package com.rimaro.musify.data.remote

import com.rimaro.musify.domain.model.MusicBrainzRecordinRes
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicBrainzApi {
    @GET("recording/")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("fmt") format: String = "json"
    ): MusicBrainzRecordinRes
}