package com.rimaro.musify.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import javax.inject.Singleton

@Singleton
object ThumbnailCreator {
    fun createPlaylistThumbnail(covers: List<Bitmap>, size: Int = 512): Bitmap {
        require(covers.size >= 4) { "Need at least 4 covers" }

        val result = createBitmap(size, size)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val half = size / 2
        val positions = listOf(
            Rect(0, 0, half, half),          // Top-left
            Rect(half, 0, size, half),       // Top-right
            Rect(0, half, half, size),       // Bottom-left
            Rect(half, half, size, size)     // Bottom-right
        )

        covers.take(4).forEachIndexed { index, cover ->
            // Scale and crop each cover to fit its quadrant (center-crop)
            val scaled = centerCropBitmap(cover, half, half)
            canvas.drawBitmap(scaled, null, positions[index], paint)
            scaled.recycle()
        }

        return result
    }

    fun centerCropBitmap(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val sourceRatio = source.width.toFloat() / source.height
        val targetRatio = targetW.toFloat() / targetH

        val (cropW, cropH) = if (sourceRatio > targetRatio) {
            // Source is wider — crop sides
            ((source.height * targetRatio).toInt()) to source.height
        } else {
            // Source is taller — crop top/bottom
            source.width to ((source.width / targetRatio).toInt())
        }

        val x = (source.width - cropW) / 2
        val y = (source.height - cropH) / 2

        val cropped = Bitmap.createBitmap(source, x, y, cropW, cropH)
        return cropped.scale(targetW, targetH)
            .also { if (it != cropped) cropped.recycle() }
    }
}