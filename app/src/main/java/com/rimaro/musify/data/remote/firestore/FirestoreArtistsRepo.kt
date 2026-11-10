package com.rimaro.musify.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.rimaro.musify.data.remote.firestore.model.FirestoreFollowedArtist
import com.rimaro.musify.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotSignedInException : IllegalStateException("User is not signed in")

class FirestoreArtistsRepo @Inject constructor(
    private val firestore: FirebaseFirestore,
    @AppScope private val appScope: CoroutineScope,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currUid: String? get() = auth.currentUser?.uid

    private val uidFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    // references
    private fun userDoc(uid: String): DocumentReference =
        db.collection("users").document(uid)

    private fun artistsCol(uid: String): CollectionReference =
        userDoc(uid).collection("followedArtists")

    private fun requireUid(): String = currUid ?: throw NotSignedInException()

    // collection functions
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeArtists(): Flow<List<FirestoreFollowedArtist>> = uidFlow
        .onEach { Log.d("Repo", "uid = $it") }
        .flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else artistsCol(uid).snapshots()
            .onEach { Log.d("Repo", "snapshot: ${it.size()} docs, fromCache=${it.metadata.isFromCache}") }
            .map { it.toObjects(FirestoreFollowedArtist::class.java) }
    }

    suspend fun addArtist(artistId: Long) {
        artistsCol(requireUid()).document(artistId.toString())
            .set(mapOf("artistId" to artistId, "addedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun removeArtist(artistId: Long) {
        artistsCol(requireUid())
            .document(artistId.toString())
            .delete()
            .await()
    }
}