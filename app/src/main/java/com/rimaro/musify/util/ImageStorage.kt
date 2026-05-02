package com.rimaro.musify.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Singleton

@Singleton
object ImageStorage {
    private const val DIR = "playlists_thumbnail"

    fun save(context: Context, bitmap: Bitmap, filename: String): String? {
        return try {
            val dir = File(context.filesDir, DIR).also { it.mkdirs() }
            val file = File(dir, "$filename.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun load(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun delete(path: String): Boolean {
        return File(path).takeIf { it.exists() }?.delete() ?: false
    }
}