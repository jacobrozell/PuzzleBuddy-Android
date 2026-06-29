package com.jacobrozell.puzzlebuddy.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object ImageCompression {
    private const val JPEG_QUALITY = 30 // matches iOS compressionQuality 0.30

    fun compressJpeg(source: ByteArray, maxDimension: Int = 1200): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(source, 0, source.size) ?: return source
        val scaled = scaleDown(bitmap, maxDimension)
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        if (scaled !== bitmap) scaled.recycle()
        bitmap.recycle()
        return output.toByteArray()
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val largest = maxOf(width, height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
