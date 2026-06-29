package com.rimaro.musify.util.playlist_import

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistDao
import com.rimaro.musify.domain.repository.DeezerRepository
import com.rimaro.musify.ui.library.ImportResult
import com.rimaro.musify.util.thumbnail.StorageManager
import com.rimaro.musify.util.thumbnail.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistImporter @Inject constructor(
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
    ): Flow<ImportResult> = flow {
        val inputStream = application.contentResolver.openInputStream(uri)
            ?: run {
                emit(ImportResult.Error("Could not open file")); return@flow
            }

        val playlistId = createPlaylist(uri)
        if (playlistId == null) {
            emit(ImportResult.Error("Error creating the playlist"))
            return@flow
        }

        var processed = 0
        var failed = 0
        val resolvedIds = mutableListOf<Long>()
        val covers = mutableListOf<String>()

        CsvManager.parseCsvStream(inputStream)
            .chunked(50)
            .forEach { chunk ->
                val tracks = coroutineScope {
                    chunk.map { track ->
                        val query = "${track.title} - ${track.artist}"
                        Log.d("NewPlaylist", "Query: $query")
                        async { deezerRepository.searchTrack(query, limit = 1).data.firstOrNull() }
                    }.awaitAll()
                }

                tracks.forEach { track ->
                    if (track != null) resolvedIds.add(track.id) else failed++
                    if (covers.size < 4) {
                        track?.album?.coverXl?.let {
                            covers.add(it)
                        }
                    }
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

        // create thumbnail
        val thumbnailPath = createPlaylistThumbnail(covers, playlistId)
        if (thumbnailPath == null) {
            emit(ImportResult.Error("Error creating playliust"))
            return@flow
        }

        // update playlist with thumbnail
        firestorePlaylistDao.updatePlaylistThumbnail(playlistId, thumbnailPath)

        emit(ImportResult.Success(imported = processed - failed, skipped = failed))
    }.flowOn(Dispatchers.IO)

    private suspend fun createPlaylist(uri: Uri): String? {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val fileName = uri.getFileName(application)?.split(".csv")[0] ?: "New Playlist"
        val playlistId =  firestorePlaylistDao.createPlaylist(
            ownerId = userId,
            name = fileName
        )

        return playlistId
    }

    suspend fun createPlaylistThumbnail(covers: List<String>, fileName: String): String? {
        val bitmaps = coroutineScope {
            covers.take(4)
                .map { uri -> async { loadBitmapFromUri(application, uri.toUri()) } }
                .awaitAll()
                .filterNotNull()
        }

        // if less than 4 tracks, use the first track cover
        val thumbnailBitmap = if (bitmaps.size < 4) {
            bitmaps[0]
        } else {
            ThumbnailManager.createPlaylistThumbnail(bitmaps)
        }
        val thumbnailPath = StorageManager.save(application, thumbnailBitmap, fileName)

        return thumbnailPath
    }

    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return withContext(Dispatchers.IO) {
            Glide.with(context)
                .asBitmap()
                .load(uri)
                .submit()
                .get()
        }
    }

}