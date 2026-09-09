package com.rimaro.musify.data.remote.firestore

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.rimaro.musify.di.AppScope
import com.rimaro.musify.domain.model.FirestoreTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreLikedTracksRepo @Inject constructor(
    private val firestore: FirebaseFirestore,
    @AppScope private val appScope: CoroutineScope
) {
    private val auth = Firebase.auth

    val likedTracks: StateFlow<Set<FirestoreTrack>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptySet())
            awaitClose { } // no listener to remove, but must still call awaitClose
            return@callbackFlow
        }

        val listener = firestore.collection(USERS_COLLECTION).document(uid)
            .collection(LIKED_TRACKS_COLLECTION)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Log.e("LikedTracksDebug", "uid=$uid, error=${exception.message}")
                    close(exception); return@addSnapshotListener
                }
                trySend(snapshots?.toObjects(FirestoreTrack::class.java)?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }.stateIn(appScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun addTrack(track: FirestoreTrack) {
        val uid = auth.currentUser?.uid ?: return
        appScope.launch {
            firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(LIKED_TRACKS_COLLECTION)
                .add(track)
                .await()
        }
    }

    fun removeTrack(trackId: Long) {
        appScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch

            val collectionRef = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .collection(LIKED_TRACKS_COLLECTION)

            val snapshot = collectionRef
                .whereEqualTo("trackId", trackId)
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val LIKED_TRACKS_COLLECTION = "likedTracks"
    }
}