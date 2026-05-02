package com.rimaro.musify.data.remote.firestore

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rimaro.musify.domain.model.FirestorePlaylist
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestorePlaylistDao @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val PLAYLISTS_COLLECTION = "playlists"
        private const val BATCH_LIMIT = 500
    }

    // --- Playlist CRUD ---

    suspend fun createPlaylist(ownerId: String, name: String): String {
        val docRef = firestore.collection(PLAYLISTS_COLLECTION).document()
        val data = mapOf(
            "id"         to docRef.id,
            "ownerId"    to ownerId,
            "name"       to name,
            "trackIds"   to emptyList<String>(),
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

        return snapshot.toPlaylist()
    }

    suspend fun getUserPlaylists(userId: String): List<FirestorePlaylist> {
        val snapshot = firestore
            .collection(PLAYLISTS_COLLECTION)
            .whereEqualTo("ownerId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toPlaylist() }
    }

    suspend fun deletePlaylist(playlistId: String) {
        firestore.collection(PLAYLISTS_COLLECTION)
            .document(playlistId)
            .delete()
            .await()
    }

    suspend fun updatePlaylistThumbnail(playlistId: String, thumbnailPath: String) {
        firestore.collection((PLAYLISTS_COLLECTION))
            .document(playlistId)
            .update(
                "thumbnailPath", thumbnailPath
            )
            .await()
    }

    // --- Track ID management ---

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

    // --- Helpers ---

    private fun DocumentSnapshot.toPlaylist(): FirestorePlaylist? {
        return try {
            FirestorePlaylist(
                id       = getString("id") ?: return null,
                ownerId  = getString("ownerId") ?: return null,
                name     = getString("name") ?: return null,
                trackIds = (get("trackIds") as? List<*>)
                    ?.mapNotNull { (it as? String) }
                    ?: emptyList(),
            )
        } catch (e: Exception) {
            null
        }
    }
}