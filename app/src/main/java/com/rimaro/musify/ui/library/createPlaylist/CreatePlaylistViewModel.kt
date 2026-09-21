package com.rimaro.musify.ui.library.createPlaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePlaylistViewModel @Inject constructor(
    private val firestorePlaylistRepo: FirestorePlaylistRepo
) : ViewModel() {
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            firestorePlaylistRepo.createPlaylist(name)
        }
    }
}