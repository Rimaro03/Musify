package com.rimaro.musify.data.remote.firestore

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rimaro.musify.di.AppScope
import com.rimaro.musify.data.remote.firestore.model.FirestoreTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreLikedTracksRepo @Inject constructor(
    private val firestore: FirebaseFirestore,
    @AppScope private val appScope: CoroutineScope
) {
    private val auth = Firebase.auth
    private val uid: String
        get() = auth.currentUser?.uid ?: error("User not signed in")

    private val userDoc
        get() = firestore.collection(USERS_COLLECTION).document(uid)

    private val likedTracksCollection
        get() = userDoc.collection(LIKED_TRACKS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val LIKED_TRACKS_COLLECTION = "likedTracks"
    }

    val likedTracks: StateFlow<Set<FirestoreTrack>> = callbackFlow {
        val listener = likedTracksCollection
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Log.e("LikedTracksDebug", "uid=$uid, error=${exception.message}")
                    close(exception);
                    return@addSnapshotListener
                }
                trySend(snapshots?.toObjects(FirestoreTrack::class.java)?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }.stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private suspend fun addTrack(trackId: Long) {
        likedTracksCollection.document(trackId.toString())
            .set(mapOf("trackId" to trackId, "addedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    private suspend fun removeTrack(trackId: Long) {
        likedTracksCollection
            .document(trackId.toString())
            .delete()
            .await()
    }

    suspend fun toggleLike(trackId: Long) {
        if(isLiked(trackId)) removeTrack(trackId)
        else addTrack(trackId)
    }

    fun isLiked(trackId: Long): Boolean {
        return likedTracks.value.map { it.trackId }.contains(trackId)
    }

}