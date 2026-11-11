package com.rimaro.musify.ui.library.albums

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.deezer.dto.DeezerAlbum
import com.rimaro.musify.data.remote.firestore.FirestoreAlbumsRepo
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
class AlbumsViewModel @Inject constructor(
    private val firestoreAlbumsRepo: FirestoreAlbumsRepo,
    private val deezerRepository: DeezerRepository
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val albumsUiState: StateFlow<AlbumsUiState> = firestoreAlbumsRepo
        .observeAlbums()
        .transformLatest {
            try {
                emit(AlbumsUiState.Loading)
                val albums = fetchAlbums(it.map { album -> album.albumId })

                emit(AlbumsUiState.Success(albums))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("AlbumsViewModel", e.message ?: "")
                emit(AlbumsUiState.Error(e.message ?: ""))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AlbumsUiState.Loading
        )

    private suspend fun fetchAlbums(albumIds: List<Long>) : List<DeezerAlbum> = coroutineScope{
        val semaphore = Semaphore(5)
        albumIds.map { id ->
            async {
                semaphore.withPermit { deezerRepository.getAlbumById(id) }
            }
        }.awaitAll()
    }
}