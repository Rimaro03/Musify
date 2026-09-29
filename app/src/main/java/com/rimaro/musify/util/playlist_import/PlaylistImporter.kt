package com.rimaro.musify.util.playlist_import

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.rimaro.musify.data.remote.firestore.FirestorePlaylistRepo
import com.rimaro.musify.data.repository.DeezerRepository
import com.rimaro.musify.ui.library.ImportResult
import com.rimaro.musify.util.thumbnail.StorageManager
import com.rimaro.musify.util.thumbnail.ThumbnailManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistImporter @Inject constructor(
    private val application: Application,
    private val deezerRepository: DeezerRepository,
    private val firestorePlaylistRepo: FirestorePlaylistRepo
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

    suspend fun importFromCsv(uri: Uri): ImportResult {
        val inputStream = application.contentResolver.openInputStream(uri)
            ?: run {
                return ImportResult.Error("Could not open file")
            }

        val playlistId =
            createPlaylist(uri) ?: return ImportResult.Error("Error creating the playlist")

        var succeeded = 0
        var failed = 0
        val resolvedTracks = mutableListOf<Long>()
        val covers = mutableListOf<String>()

        CsvManager.parseCsvStream(inputStream)
            .chunked(20)
            .forEach { csvChunk ->
                val res = coroutineScope {
                    csvChunk.map { track ->
                        val query = "${track.title} - ${track.artist}"
                        async {
                            deezerRepository
                                .searchTrack(query, limit = 1)
                                .data.firstOrNull()?.id
                        }
                    }
                }.awaitAll()

                succeeded += res.filterNotNull().size
                failed += res.filter { it == null }.size
                resolvedTracks += res.filterNotNull()
            }
        withContext(Dispatchers.IO) {
            inputStream.close()
        }

        // Flush to Firestore every 500 tracks
        resolvedTracks.chunked(500).forEach { chunk ->
            firestorePlaylistRepo.addTrackIdsBatch(playlistId, chunk)
        }

        // create thumbnail
        val thumbnailPath = createPlaylistThumbnail(covers, playlistId)
        if (thumbnailPath == null) {
            Log.e("PlaylistImporter", "Failed to create playlist thumbnail")
        }

        // update playlist with thumbnail
        firestorePlaylistRepo.updatePlaylistThumbnail(playlistId, thumbnailPath ?: "")

        return ImportResult.Success(imported = succeeded, skipped = failed)
    }

    private suspend fun createPlaylist(uri: Uri): String? {
        val fileName = uri.getFileName(application)?.split(".csv")[0] ?: "New Playlist"
        val playlistId =  firestorePlaylistRepo.createPlaylist(
            name = fileName
        )

        return playlistId
    }

    suspend fun createPlaylistThumbnail(covers: List<String>, fileName: String): String? {
        if(covers.size < 4) return ""
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