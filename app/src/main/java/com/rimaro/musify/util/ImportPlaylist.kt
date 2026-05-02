package com.rimaro.musify.util

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.ui.library.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportPlaylist @Inject constructor(
    private val application: Application,
    private val deezerRepository: DeezerRepository,
    private val firestorePlaylistDao: FirestorePlaylistDao
) {
    companion object {
        private const val BATCH_LIMIT = 500
    }

    fun Uri.getFileName(context: Context): String? {
        return context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    fun importFromCsv(
        uri: Uri
    ): Flow<ImportResult> = flow{
        val inputStream = application.contentResolver.openInputStream(uri)
            ?: run {
                emit(ImportResult.Error("Could not open file")); return@flow
            }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            emit(ImportResult.Error("UserId is null"))
            return@flow
        }
        val fileName = uri.getFileName(application)?.split(".csv")[0] ?: "New Playlist"
        val playlistId =  firestorePlaylistDao.createPlaylist(
            ownerId = userId,
            name = fileName
        )

        var processed = 0
        var failed = 0
        val resolvedIds = mutableListOf<Long>()

        parseCsvStream(inputStream)
            .chunked(50)
            .forEach { chunk ->
                val ids = coroutineScope {
                    chunk.map { track ->
                        val query = "${track.title} - ${track.artist}"
                        Log.d("NewPlaylist", "Query: $query")
                        async { deezerRepository.searchTrack(query, limit = 1).data.firstOrNull()?.id }
                    }.awaitAll()
                }

                ids.forEach { id ->
                    if (id != null) resolvedIds.add(id) else failed++
                }

                processed += chunk.size
                emit(ImportResult.Progress(processed, -1, failed)) // -1 = total unknown (streaming)

                // Flush to Firestore every 500 resolved IDs
                if (resolvedIds.size >= BATCH_LIMIT) {
                    Log.d("NewPlaylist", "Flushing to firestore, BATCH LIMIT")
                    firestorePlaylistDao.addTrackIdsBatch(playlistId, resolvedIds.toList())
                    resolvedIds.clear()
                }
            }

        // Flush remaining
        if (resolvedIds.isNotEmpty()) {
            firestorePlaylistDao.addTrackIdsBatch(playlistId, resolvedIds.toList())
            Log.d("NewPlaylist", "Flushing to firestore")
        }

        emit(ImportResult.Success(imported = processed - failed, skipped = failed))
    }.flowOn(Dispatchers.IO)

}