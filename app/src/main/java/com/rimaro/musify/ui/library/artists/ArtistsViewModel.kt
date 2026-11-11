package com.rimaro.musify.ui.library.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist
import com.rimaro.musify.data.remote.firestore.FirestoreArtistsRepo
import com.rimaro.musify.data.repository.DeezerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val firestoreArtistsRepo: FirestoreArtistsRepo,
    private val deezerRepository: DeezerRepository
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val artistsUiState: StateFlow<ArtistsUiState> = firestoreArtistsRepo
        .observeArtists()
        .transformLatest {
            try {
                emit(ArtistsUiState.Loading)
                val artists = fetchArtists(it.map { artist -> artist.artistId })

                emit(ArtistsUiState.Success(artists))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ArtistsViewModel", e.message ?: "")
                emit(ArtistsUiState.Error(e.message ?: ""))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtistsUiState.Loading
        )

    private suspend fun fetchArtists(artistIds: List<Long>) : List<DeezerArtist> = coroutineScope{
        val semaphore = Semaphore(5)
        artistIds.map { id ->
            async {
                semaphore.withPermit { deezerRepository.getArtistById(id) }
            }
        }.awaitAll()
    }
}