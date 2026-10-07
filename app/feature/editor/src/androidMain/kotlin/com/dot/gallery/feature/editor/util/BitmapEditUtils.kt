/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.editor.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

/**
 * Apply a full 4×5 color matrix to a [Bitmap] using Android's native
 * [android.graphics.ColorMatrixColorFilter].  This produces pixel-identical
 * results to Compose's [androidx.compose.ui.graphics.ColorFilter.colorMatrix].
 */
fun applyColorMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
    val result = createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return result
}

fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int

    if (width > height) {
        newWidth = maxWidth
        newHeight = (maxWidth / aspectRatio).toInt()
    } else {
        newHeight = maxHeight
        newWidth = (maxHeight * aspectRatio).toInt()
    }

    return bitmap.scale(newWidth, newHeight)
}

fun overlayBitmaps(currentImage: Bitmap, markupBitmap: Bitmap): Bitmap {
    // Create a new bitmap with the same dimensions as the current image
    val resultBitmap = createBitmap(
        currentImage.width,
        currentImage.height,
        currentImage.config ?: Bitmap.Config.ARGB_8888
    )

    // Create a canvas to draw on the new bitmap
    val canvas = Canvas(resultBitmap)

    // Draw the current image on the canvas
    canvas.drawBitmap(currentImage, 0f, 0f, null)

    // Draw the markup bitmap on top of the current image
    canvas.drawBitmap(markupBitmap.copy(Bitmap.Config.ARGB_8888, true), 0f, 0f, null)

    return resultBitmap
}

fun Bitmap.flipHorizontally(): Bitmap {
    val matrix = Matrix().apply { postScale(-1f, 1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.flipVertically(): Bitmap {
    val matrix = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
