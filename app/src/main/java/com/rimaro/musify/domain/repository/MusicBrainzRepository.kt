package com.rimaro.musify.domain.repository

import com.rimaro.musify.data.remote.MusicBrainzApi
import javax.inject.Inject

class MusicBrainzRepository @Inject constructor(
    private val musicBrainzApi: MusicBrainzApi
){
//    suspend fun searchTracks(query: String) : List<Track> {
//        val apiQuery = "recordings:\"$query\""
//        val res = musicBrainzApi.searchRecordings(apiQuery)
//        return res.recordings.map { it.toDomain() }
//    }
}