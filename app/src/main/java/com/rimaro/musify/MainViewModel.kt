package com.rimaro.musify

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val firestorePlaylistDao: FirestorePlaylistDao,
) : AndroidViewModel(application) {
    private val auth = Firebase.auth

    private val _likedTrackIds = MutableStateFlow<List<Long>>(emptyList())
    val likedTrackIds: StateFlow<List<Long>> = _likedTrackIds

    init {
        fetchUserLikedTracks()
    }

    private fun fetchUserLikedTracks() {
        viewModelScope.launch {
            auth.currentUser?.let { user ->
                val likedTracks = firestorePlaylistDao.getUserLikedTracks(user.uid)
                _likedTrackIds.value = likedTracks.map { it.trackId }
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}