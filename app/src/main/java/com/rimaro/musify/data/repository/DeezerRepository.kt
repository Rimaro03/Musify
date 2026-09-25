package com.rimaro.musify.data.repository

import com.rimaro.musify.data.remote.deezer.DeezerApiService
import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist
import com.rimaro.musify.data.remote.deezer.dto.DeezerAutocompleteRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerGenre
import com.rimaro.musify.data.remote.deezer.dto.DeezerSearchRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerSearchTrackRes
import com.rimaro.musify.data.remote.deezer.dto.DeezerTrack
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

class DeezerRepository @Inject constructor(
    private val deezerApiService: DeezerApiService
) {
    suspend fun search(query: String) : DeezerSearchRes {
        return deezerApiService.search(query)
    }

    suspend fun searchTrack(
        query: String,
        limit: Int = 25,
        offset: Int = 0
    ) : DeezerSearchTrackRes {
        return deezerApiService.searchTrack(
            query,
            limit,
            offset
        )
    }

    suspend fun autocomplete(query: String) : DeezerAutocompleteRes {
        return deezerApiService.autocomplete(query)
    }

    suspend fun getTopArtists(): List<DeezerArtist> {
        return deezerApiService.topArtists().data
    }

    suspend fun getGenres(): List<DeezerGenre> {
        return deezerApiService.genres().data
    }

    suspend fun getTrackById(trackId: Long): DeezerTrack {
        return deezerApiService.track(trackId.toString())
    }

    suspend fun getTrackByIds(trackIds: List<Long>, batchSize: Int = 20): List<DeezerTrack> {
        return trackIds .chunked(batchSize).flatMap { chunk ->
            coroutineScope {
                chunk.map { id ->
                    async { runCatching { getTrackById(id) }.getOrNull() }
                }.awaitAll().filterNotNull()
            }
        }
    }
}