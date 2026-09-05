package com.rimaro.musify.domain.repository

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.rimaro.musify.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LikedTracksRepo @Inject constructor(
    private val firestore: FirebaseFirestore,
    @AppScope private val appScope: CoroutineScope
) {
    private val auth = Firebase.auth

    val likedTrackIds: StateFlow<Set<String>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val listener = firestore.collection("users").document(uid)
            .collection("likedTracks")
            .addSnapshotListener { snapshots, exception ->
                if(exception != null) { close(exception); return@addSnapshotListener }
                trySend(snapshots?.documents?.map { (it.data?.get("trackId") as Long).toString()}?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }.stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}