package com.rimaro.musify.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.rimaro.musify.data.remote.firestore.model.FirestoreFollowedAlbums
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
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreAlbumsRepo @Inject constructor(
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

    private fun albumsCol(uid: String): CollectionReference =
        userDoc(uid).collection("likedAlbums")

    private fun requireUid(): String = currUid ?: throw NotSignedInException()

    // collection functions
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAlbums(): Flow<List<FirestoreFollowedAlbums>> = uidFlow
        .flatMapLatest { uid ->
        if (uid == null) flowOf(emptyList())
        else albumsCol(uid).snapshots()
            .map { it.toObjects(FirestoreFollowedAlbums::class.java) }
    }

    suspend fun addAlbum(albumId: Long) {
        albumsCol(requireUid()).document(albumId.toString())
            .set(mapOf("albumId" to albumId, "addedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun removeAlbum(albumId: Long) {
        albumsCol(requireUid())
            .document(albumId.toString())
            .delete()
            .await()
    }
}