package com.rimaro.musify.ui.library

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.util.ImportPlaylist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    application: Application,
    private val importPlaylist: ImportPlaylist,
    private val firestorePlaylistDao: FirestorePlaylistDao
) : AndroidViewModel(application) {
    private val _libraryUiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Idle)
    val libraryUiState: StateFlow<LibraryUiState> = _libraryUiState

    val importState = MutableStateFlow<ImportResult?>(null)

    init {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        getUserPlaylists(userId)
    }

    private fun getUserPlaylists(userId: String?) {
        if(userId == null) {
            _libraryUiState.value = LibraryUiState.Error("User is null")
            return
        }
        viewModelScope.launch {
            val playlists = firestorePlaylistDao.getUserPlaylists(userId)
            _libraryUiState.value = LibraryUiState.Success(playlists)
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            importPlaylist.importFromCsv(uri)
                .collect { result ->
                    importState.value = result
                }
        }
    }
}
