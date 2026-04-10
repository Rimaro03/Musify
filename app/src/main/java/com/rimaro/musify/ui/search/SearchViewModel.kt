package com.rimaro.musify.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.local.preferences.SearchHistoryManager
import com.rimaro.musify.domain.model.DeezerAutocompleteRes
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val deezerRepository: DeezerRepository,
    private val historyManager: SearchHistoryManager,
    private val trackUrlResolver: TrackUrlResolver,
    private val musicRepository: PlayerController,
    private val playerController: PlayerController
) : AndroidViewModel(application) {
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState = _searchUiState.asStateFlow()

    private val _trendingUiState = MutableStateFlow<TrendingUiState>(TrendingUiState.Idle)
    val trendingUiState = _trendingUiState.asStateFlow()

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

    fun onClick(track: Track) {
        viewModelScope.launch {
            track.streamUrl?.let {
                musicRepository.playTracks(listOf(track))
            }
        }
    }

    /* SEARCH LOGIC */
    private fun performSearch(query: String) {
        if(query.isBlank()) return

        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val res = deezerRepository.autocomplete(query)
                val tracks = res.tracks.data.map { it.toTrack() }

                val searchResultList = buildSearchItemsList(res, tracks)
                if(searchResultList.isEmpty()) {
                    _searchUiState.value = SearchUiState.Error("No track found")
                } else {
                    _searchUiState.value = SearchUiState.Success(searchResultList)
                }

                fetchStreamUrl(tracks).collect { fetchedTrack ->
                    val currentTracks = (_searchUiState.value as SearchUiState.Success).searchResultList.toMutableList()
                    val position = currentTracks.indexOfFirst { it is SearchResultItem.TrackItem && it.track.id == fetchedTrack.id  }
                    if(position != -1) {
                        currentTracks[position] = SearchResultItem.TrackItem(fetchedTrack)
                        _searchUiState.value = SearchUiState.Success(currentTracks.toList())
                    }
                }
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.message ?: "Unknown error")
                Log.e("SearchViewModel", "Error performing search", e)
            }
        }
    }

    private fun buildSearchItemsList(res: DeezerAutocompleteRes, tracks: List<Track>): List<SearchResultItem> {
        val searchResultList = mutableListOf<SearchResultItem>()
        // first add the most relevant artist
        if(res.artists.data.isNotEmpty()) {
            searchResultList.add(SearchResultItem.ArtistItem(res.artists.data[0]))
        }
        // then add the tracks
        if(tracks.isNotEmpty()) {
            searchResultList.addAll(tracks.map { track -> SearchResultItem.TrackItem(track) })
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
            } catch (e: Exception) {
                _trendingUiState.value = TrendingUiState.Error(e.message ?: "Unknown error")
                Log.e("SearchViewModel", "Error getting trending artists", e)
            }
        }
    }

    /* TRACK STREAM URL FETCHING */
    fun fetchStreamUrl(tracks: List<Track>): Flow<Track> = channelFlow {
        tracks.map { track ->
            async {
                val fetchedTrack = trackUrlResolver.resolve(track)
                fetchedTrack?.let { send(it) }
            }
        }.awaitAll()
    }

    /* PLAYER LOGIC */
    fun enqueueTracks(tracks: List<Track>) = playerController.enqueueTracks(tracks)
}