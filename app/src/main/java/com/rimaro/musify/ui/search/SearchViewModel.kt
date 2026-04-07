package com.rimaro.musify.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.local.preferences.SearchHistoryManager
import com.rimaro.musify.domain.model.DeezerAutocompleteRes
import com.rimaro.musify.domain.model.DeezerTrack
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.repository.MusicRepository
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val deezerRepository: DeezerRepository,
    private val historyManager: SearchHistoryManager,
    private val trackUrlResolver: TrackUrlResolver,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState = _searchUiState.asStateFlow()

    private val _trendingUiState = MutableStateFlow<TrendingUiState>(TrendingUiState.Idle)
    val trendingUiState = _trendingUiState.asStateFlow()

    var tracks: List<Track> = emptyList()

    init {
        getTrendingArtistsAndGenres()
    }

    /* HISTORY LOGIC */
    val history = historyManager.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearch(query: String) {
        viewModelScope.launch { historyManager.add(query) }
        performSearch(query)
    }

    fun removeQuery(query: String) {
        viewModelScope.launch { historyManager.remove(query) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyManager.clearAll() }
    }

    fun onClick(deezerTrack: DeezerTrack) {
        viewModelScope.launch {
            val track = tracks.find { it.id == deezerTrack.id }
            if (track == null) {
                Log.e("SearchViewModel", "Track not found in tracks list")
                return@launch
            }
            Log.d("SearchViewModel", "Enqueueing track: $track")

            musicRepository.playTracks(listOf(track))
        }
    }

    /* SEARCH LOGIC */
    private fun performSearch(query: String) {
        if(query.isBlank()) return

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val res = deezerRepository.autocomplete(query)
                tracks = fetchStreamUrl(res.tracks.data)

                val searchResultList = buildSearchItemsList(res)
                if(searchResultList.isEmpty()) {
                    _searchUiState.value = SearchUiState.Error("No track found")
                } else {
                    _searchUiState.value = SearchUiState.Success(searchResultList)
                }
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: "Unknown error")
                Log.e("SearchViewModel", "Error performing search", e)
            }
        }
    }

    private fun buildSearchItemsList(res: DeezerAutocompleteRes): List<SearchResultItem> {
        val searchResultList = mutableListOf<SearchResultItem>()
        // first add the most relevant artist
        if(res.artists.data.isNotEmpty()) {
            searchResultList.add(SearchResultItem.ArtistItem(res.artists.data[0]))
        }
        // then add the tracks
        if(res.tracks.data.isNotEmpty()) {
            searchResultList.addAll(res.tracks.data.map { track -> SearchResultItem.TrackItem(track) })
        }
        // then add the most relevant album
        if(res.albums.data.isNotEmpty()) {
            searchResultList.add(SearchResultItem.AlbumItem(res.albums.data[0]))
        }

        return searchResultList
    }

    /* TRENDING ARTISTS/GENRES LOGIC */
    fun getTrendingArtistsAndGenres() {
        viewModelScope.launch {
            _trendingUiState.value = TrendingUiState.Loading
            try {
                val artistsFuture = async { deezerRepository.getTopArtists() }
                val genresFuture = async { deezerRepository.getGenres() }

                val artists = artistsFuture.await()
                val genres = genresFuture.await()

                _trendingUiState.value = TrendingUiState.Success(
                    artists,
                    genres
                )
                Log.d("SearchViewModel", "Received ${artists.size} artists" +
                        "and ${genres.size} genres")
            } catch (e: Exception) {
                _trendingUiState.value = TrendingUiState.Error(e.message ?: "Unknown error")
                Log.e("SearchViewModel", "Error getting trending artists", e)
            }
        }
    }

    /* TRACK STREAM URL FETCHING */
    suspend fun fetchStreamUrl(deezerTracks: List<DeezerTrack>): List<Track> = coroutineScope {
        deezerTracks.map {
            async { trackUrlResolver.resolve(it) }
        }.awaitAll().filterNotNull()
    }
}