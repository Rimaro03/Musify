package com.rimaro.musify.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.local.preferences.SearchHistoryManager
import com.rimaro.musify.data.remote.firestore.FirestoreLikedTracksRepo
import com.rimaro.musify.domain.model.DeezerAutocompleteRes
import com.rimaro.musify.domain.model.Track
import com.rimaro.musify.domain.model.toTrack
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.player.controller.PlayerController
import com.rimaro.musify.player.controller.PreviewPlayerController
import com.rimaro.musify.resolver.TrackUrlResolver
import com.rimaro.musify.ui.common.model.TrackUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    application: Application,
    private val deezerRepository: DeezerRepository,
    private val historyManager: SearchHistoryManager,
    private val trackUrlResolver: TrackUrlResolver,
    private val playerController: PlayerController,
    private val previewPlayerController: PreviewPlayerController,
    private val likedTracksRepo: FirestoreLikedTracksRepo
) : AndroidViewModel(application) {
    /** Handles Idle, Loading, Success, Error states */
    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    private val currentTrack: StateFlow<Track?> = playerController.currentTrack
    private val playingPlaylistId: StateFlow<String?> = playerController.playingPlaylistId
    private val audioTrackUrls: MutableStateFlow<Map<String, String>> = MutableStateFlow(emptyMap())

    var uiState: Flow<SearchUiState> =
        combine(_searchState, currentTrack, playingPlaylistId, likedTracksRepo.likedTracks, audioTrackUrls) {
            rawState, currTrack, playingPlaylistId, likedTrackIds, trackUrls ->
            when (rawState) {
                is SearchUiState.Success -> {
                    val thisPlaylistActive = playingPlaylistId == null
                    val state = SearchUiState.Success(
                        searchResultList = rawState.searchResultList.map { resultItem ->
                            if(resultItem is SearchResultItem.TrackItem) {
                                val newTrackModel = resultItem.trackModel.copy(
                                    track = resultItem.trackModel.track.copy(
                                        streamUrl = trackUrls[resultItem.trackModel.track.id.toString()]
                                    ),
                                    isPlaying = thisPlaylistActive && currTrack?.id == resultItem.trackModel.track.id,
                                    isLiked = likedTrackIds
                                        .map{ it.trackId }
                                        .contains(resultItem.trackModel.track.id)
                                )
                                resultItem.copy(
                                    trackModel = newTrackModel
                                )
                            } else resultItem
                        }
                    )
                    state
                }
                is SearchUiState.Idle -> SearchUiState.Idle
                is SearchUiState.Loading -> SearchUiState.Loading
                is SearchUiState.Error -> SearchUiState.Error(rawState.message)
        }
    }

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

    fun playTrack(track: Track) {
        viewModelScope.launch {
            previewPlayerController.stop()
            track.streamUrl?.let {
                playerController.playTracks(listOf(track), null)
            }
        }
    }

    fun playPreview(track: Track) {
        playerController.pause()
        track.previewUrl?.let {
            previewPlayerController.playPreview(track.id.toString(), it)
        }
    }

    fun stopPreview() {
        previewPlayerController.stop()
    }

    /* SEARCH LOGIC */
    private fun performSearch(query: String) {
        if(query.isBlank()) return

        viewModelScope.launch {
            _searchState.value = SearchUiState.Loading
            try {
                val res = deezerRepository.autocomplete(query)
                val tracks = res.tracks.data.map {
                    TrackUiModel(it.toTrack())
                }

                val searchResultList = buildSearchItemsList(res, tracks)
                if(searchResultList.isEmpty()) {
                    _searchState.value = SearchUiState.Error("No track found")
                } else {
                    _searchState.value = SearchUiState.Success(searchResultList)
                }

                fetchStreamUrl(tracks).collect { fetchedTrack ->
                    audioTrackUrls.value += fetchedTrack
                    // TODO: move this to a repo
                }
            } catch (e: Exception) {
                _searchState.value = SearchUiState.Error(e.message ?: "Unknown error")
                Log.e("SearchViewModel", "Error performing search", e)
            }
        }
    }

    private fun buildSearchItemsList(res: DeezerAutocompleteRes, trackModels: List<TrackUiModel>): List<SearchResultItem> {
        val searchResultList = mutableListOf<SearchResultItem>()
        // first add the most relevant artist
        if(res.artists.data.isNotEmpty()) {
            searchResultList.add(SearchResultItem.ArtistItem(res.artists.data[0]))
        }
        // then add the tracks
        if(trackModels.isNotEmpty()) {
            searchResultList.addAll(trackModels.map { trackModel -> SearchResultItem.TrackItem(trackModel) })
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
    private fun fetchStreamUrl(tracks: List<TrackUiModel>): StateFlow<Map<String, String>> = channelFlow {
        tracks.map { trackModel ->
            async {
                val fetchedTrack = trackUrlResolver.resolve(trackModel.track)
                fetchedTrack?.let {
                    if(it.streamUrl != null) {
                        send(mapOf(Pair(it.id.toString(), it.streamUrl!!)))
                    }
                }
            }
        }.awaitAll()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /* PLAYER LOGIC */
    fun enqueueTracks(tracks: List<Track>) = playerController.enqueueTracks(tracks, playlistId = null)

    override fun onCleared() {
        super.onCleared()
        previewPlayerController.stop()
        playerController.stop()
    }

    /* TRACK LIKE/UNLIKE LOGIC */
    fun unlikeTrack(track: Track) = viewModelScope.launch {
        likedTracksRepo.removeTrack(track.id)
    }

    companion object {
        private const val TAG = "SearchViewModel"
    }
}