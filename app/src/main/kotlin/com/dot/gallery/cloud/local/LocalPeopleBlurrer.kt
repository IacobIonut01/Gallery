/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Environment
import com.dot.gallery.cloud.data.dao.DetectedFaceDao
import com.dot.gallery.core.MediaHandler
import com.dot.gallery.feature_node.domain.model.editor.MarkupBrush
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.edit.utils.ImageObscure
import com.dot.gallery.feature_node.presentation.util.printWarning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Batch operation that obscures a given person's face in every photo they appear in and saves the
 * results as new copies (non-destructive). Reuses the face boxes already stored by the
 * [com.dot.gallery.core.workers.FaceIndexerWorker], so no re-detection is needed.
 */
@Singleton
class LocalPeopleBlurrer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val faceDao: DetectedFaceDao,
    private val mediaRepository: MediaRepository,
    private val mediaHandler: MediaHandler
) {

    /**
     * Obscure [personId]'s faces across all their media, saving edited copies.
     * @return the number of images successfully written.
     */
    suspend fun blurPersonEverywhere(
        personId: String,
        brush: MarkupBrush = MarkupBrush.Blur,
        strength: Float = 0.6f,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        val mediaIds = faceDao.getMediaIdsForPerson(personId).toHashSet()
        if (mediaIds.isEmpty()) return 0
        val allMedia = mediaRepository.getCompleteMedia().first().data.orEmpty()
        val targets = allMedia.filter { it.id in mediaIds && it.mimeType.startsWith("image") }

        var written = 0
        targets.forEachIndexed { index, media ->
            onProgress(index, targets.size)
            try {
                val faces = faceDao.getByMedia(media.id).filter { it.personId == personId }
                if (faces.isEmpty()) return@forEachIndexed
                val src = decodeMutable(media.getUri()) ?: return@forEachIndexed
                val canvas = Canvas(src)
                faces.forEach { f ->
                    val left = (f.left * src.width).toInt().coerceIn(0, src.width - 1)
                    val top = (f.top * src.height).toInt().coerceIn(0, src.height - 1)
                    val right = (f.right * src.width).toInt().coerceIn(left + 1, src.width)
                    val bottom = (f.bottom * src.height).toInt().coerceIn(top + 1, src.height)
                    val crop = Bitmap.createBitmap(src, left, top, right - left, bottom - top)
                    val processed = when (brush) {
                        MarkupBrush.Mosaic -> ImageObscure.mosaic(crop, strength)
                        else -> ImageObscure.blur(crop, strength)
                    }
                    canvas.drawBitmap(processed, null, Rect(left, top, right, bottom), null)
                    if (processed != crop) processed.recycle()
                    crop.recycle()
                }
                val saved = mediaHandler.saveImage(
                    bitmap = src,
                    format = Bitmap.CompressFormat.JPEG,
                    mimeType = "image/jpeg",
                    relativePath = Environment.DIRECTORY_PICTURES + "/Edited",
                    displayName = "${media.label.substringBeforeLast('.')}_blurred.jpg"
                )
                src.recycle()
                if (saved != null) written++
            } catch (e: Exception) {
                printWarning("LocalPeopleBlurrer: failed for media ${media.id}: ${e.message}")
            }
        }
        onProgress(targets.size, targets.size)
        return written
    }

    private fun decodeMutable(uri: android.net.Uri): Bitmap? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inMutable = true }
            BitmapFactory.decodeStream(input, null, opts)
        }
    } catch (e: Exception) {
        printWarning("LocalPeopleBlurrer: decode failed: ${e.message}")
        null
    }
}
