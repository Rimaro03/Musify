package com.rimaro.musify.data.remote.firestore

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.rimaro.musify.data.remote.firestore.model.FirestorePlaylist
import com.rimaro.musify.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestorePlaylistRepo @Inject constructor(
    private val firestore: FirebaseFirestore,
    @AppScope private val appScope: CoroutineScope
) {
    private val auth = Firebase.auth
    private val uid: String
        get() = auth.currentUser?.uid ?: error("User not signed in")

    private val userDoc
        get() = firestore.collection(USERS_COLLECTION).document(uid)

    private val playlistCollection
        get() = userDoc.collection(PLAYLISTS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val PLAYLISTS_COLLECTION = "playlists"
        private const val BATCH_LIMIT = 500
    }

    // --- Playlist CRUD --- //
    suspend fun createPlaylist(name: String): String {
        val docRef = playlistCollection.document()
        val data = mapOf(
            "id"         to docRef.id,
            "name"       to name,
            "tracks"     to emptyList<Long>(),
            "createdAt"  to FieldValue.serverTimestamp(),
            "updatedAt"  to FieldValue.serverTimestamp(),
            "thumbnailPath" to ""
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun getPlaylist(playlistId: String): FirestorePlaylist? {
        val snapshot = playlistCollection
            .document(playlistId)
            .get()
            .await()

        return snapshot.toObject(FirestorePlaylist::class.java)
    }

    fun observeUserPlaylists(): Flow<List<FirestorePlaylist>> = callbackFlow {
        val registration = playlistCollection
            .addSnapshotListener { snapshots, exception ->
                if (exception != null) {
                    Log.e("FirestorePlaylistRepo", "Error fetching playlists for UID $uid")
                    close(exception)
                    return@addSnapshotListener
                }
                trySend(snapshots?.toObjects(FirestorePlaylist::class.java) ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    suspend fun deletePlaylist(playlistId: String) {
        playlistCollection
            .document(playlistId)
            .delete()
            .await()
    }

    suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailPath: String) {
        playlistCollection
            .document(playlistId)
            .update(
                "thumbnailPath", thumbnailPath
            )
            .await()
    }

    // --- Playlist Tracks Management --- //
    suspend fun addTrack(playlistId: String, trackId: Long) {
        playlistCollection
            .document(playlistId)
            .update(
                "tracks", FieldValue.arrayUnion(trackId),
                "updatedAt", FieldValue.serverTimestamp(),
            )
            .await()
    }

    suspend fun removeTrack(playlistId: String, trackId: Long) {
        playlistCollection
            .document(playlistId)
            .update(
                "tracks", FieldValue.arrayRemove(trackId),
                "updatedAt", FieldValue.serverTimestamp(),
            )
            .await()
    }

    suspend fun addTracksBatch(playlistId: String, tracks: List<Long>) {
        val playlistRef = playlistCollection
            .document(playlistId)

        tracks.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            batch.update(playlistRef, "tracks", FieldValue.arrayUnion(*chunk.toTypedArray()))
            batch.update(playlistRef, "updatedAt", FieldValue.serverTimestamp())
            batch.commit().await()
        }
    }

    fun observePlaylistTracks(playlistId: String) : Flow<FirestorePlaylist> = callbackFlow {
        val registration = playlistCollection
            .document(playlistId)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("FirestorePlaylistRepo", "Error fetching playlist $playlistId")
                    close(exception)
                    return@addSnapshotListener
                }
                if(snapshot != null && snapshot.exists()) {
                    val playlist = snapshot.toObject(FirestorePlaylist::class.java)
                    if(playlist != null) trySend(playlist)
                }
            }
        awaitClose { registration.remove() }
    }
}