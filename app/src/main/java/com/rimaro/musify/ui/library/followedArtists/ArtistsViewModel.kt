package com.rimaro.musify.ui.library.followedArtists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.rimaro.musify.data.remote.deezer.dto.DeezerArtist
import com.rimaro.musify.data.remote.firestore.FirestoreArtistsRepo
import com.rimaro.musify.data.repository.DeezerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
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
    val artistUiState: StateFlow<ArtistUiState> = firestoreArtistsRepo
        .observeArtists()
        .transformLatest {
            try {
                emit(ArtistUiState.Loading)
                val artists = fetchArtists(it.map { artist -> artist.artistId })

                emit(ArtistUiState.Success(artists))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ArtistsViewModel", e.message ?: "")
                emit(ArtistUiState.Error(e.message ?: ""))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArtistUiState.Loading
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