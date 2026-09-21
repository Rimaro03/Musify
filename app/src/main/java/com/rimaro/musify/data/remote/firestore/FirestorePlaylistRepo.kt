package com.rimaro.musify.data.remote.firestore

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rimaro.musify.domain.model.FirestorePlaylist
import com.rimaro.musify.domain.model.FirestoreTrack
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestorePlaylistRepo @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val auth = Firebase.auth
    private val uid = auth.currentUser?.uid

    companion object {
        private const val PLAYLISTS_COLLECTION = "playlists"
        private const val USERS_COLLECTION = "users"
        private const val USERS_LIKED_COLLECTION = "likedTracks"
        private const val BATCH_LIMIT = 500
    }

    // --- Playlist CRUD --- //
    suspend fun createPlaylist(name: String): String? {
        if (uid == null) return null

        val docRef = firestore.collection(PLAYLISTS_COLLECTION).document()
        val data = mapOf(
            "id"         to docRef.id,
            "ownerId"    to uid,
            "name"       to name,
            "tracks"     to emptyList<FirestoreTrack>(),
            "createdAt"  to FieldValue.serverTimestamp(),
            "updatedAt"  to FieldValue.serverTimestamp(),
            "thumbnailPath" to ""
        )
        docRef.set(data).await()
        return docRef.id
    }

    suspend fun getPlaylist(playlistId: String): FirestorePlaylist? {
        val snapshot = firestore
            .collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .get()
            .await()

        return snapshot.toObject(FirestorePlaylist::class.java)
    }

    suspend fun getUserPlaylists(userId: String): List<FirestorePlaylist> {
        val snapshot = firestore
            .collection(PLAYLISTS_COLLECTION)
            .whereEqualTo("ownerId", userId)
            //.whereNotEqualTo("placeholder", true)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toPlaylist() }
    }

    fun observeUserPlaylists(): Flow<List<FirestorePlaylist>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if(uid == null){
            trySend(emptyList())
            awaitClose {  }
            return@callbackFlow
        }

        val registration = firestore.collection(PLAYLISTS_COLLECTION)
            .whereEqualTo("ownerId", uid)
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
        firestore.collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .delete()
            .await()
    }

    suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailPath: String) {
        firestore.collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .update(
                "thumbnailPath", thumbnailPath
            )
            .await()
    }

    // --- Track ID management --- //
    suspend fun addTrackId(playlistId: String, trackId: Long) {
        firestore.collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .update(
                "trackIds", FieldValue.arrayUnion(trackId),
                "updatedAt", FieldValue.serverTimestamp(),
            )
            .await()
    }

    suspend fun removeTrackId(playlistId: String, trackId: Long) {
        firestore.collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .update(
                "trackIds", FieldValue.arrayRemove(trackId),
                "updatedAt", FieldValue.serverTimestamp(),
            )
            .await()
    }

    // Called during CSV import — flushes a batch of IDs at once
    suspend fun addTrackIdsBatch(playlistId: String, trackIds: List<Long>) {
        val playlistRef = firestore
            .collection(PLAYLISTS_COLLECTION)
            .document(playlistId)

        // Chunk in case caller passes more than BATCH_LIMIT ids at once
        trackIds.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            batch.update(playlistRef, "trackIds", FieldValue.arrayUnion(*chunk.toTypedArray()))
            batch.update(playlistRef, "updatedAt", FieldValue.serverTimestamp())
            batch.commit().await()
        }
    }

    suspend fun addTracksBatch(playlistId: String, tracks: List<FirestoreTrack>) {
        val playlistRef = firestore
            .collection(PLAYLISTS_COLLECTION)
            .document(playlistId)

        tracks.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            batch.update(playlistRef, "tracks", FieldValue.arrayUnion(*chunk.toTypedArray()))
            batch.update(playlistRef, "updatedAt", FieldValue.serverTimestamp())
            batch.commit().await()
        }
    }

    // --- Helpers ---

    private fun DocumentSnapshot.toPlaylist(): FirestorePlaylist? {
        return try {
            FirestorePlaylist(
                id       = getString("id") ?: return null,
                ownerId  = getString("ownerId") ?: return null,
                name     = getString("name") ?: return null,
                tracks = (get("tracks") as? List<FirestoreTrack>) ?: emptyList(),
                thumbnailPath = getString("thumbnailPath") ?: return null
            )
        } catch (e: Exception) {
            null
        }
    }
}