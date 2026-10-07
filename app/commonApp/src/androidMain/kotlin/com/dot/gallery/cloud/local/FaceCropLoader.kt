/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.local

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import com.dot.gallery.data.dao.CloudMediaDao
import com.dot.gallery.data.entity.DetectedFaceEntity
import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.domain.model.UriMedia
import com.dot.gallery.feature.editor.adjustments.Crop
import com.dot.gallery.core.smart.SmartThumbnailLoader


/**
 * Produces face-crop thumbnails for `detected_faces` rows: decodes the owning media
 * once per media id (cached per call batch), crops the stored normalized box, and
 * writes a small JPEG into `face_thumbs/crops/`. Files are keyed by face id +
 * result revision, so re-detection produces a fresh crop while stale files are
 * simply orphaned cache — bounded by the number of stored faces.
 */
@Singleton
class FaceCropLoader @Inject constructor(
    private val context: Context,
    private val thumbnailLoader: SmartThumbnailLoader,
    private val mediaRepository: MediaRepository,
    private val cloudMediaDao: CloudMediaDao
) {
    private val directory: File by lazy {
        File(context.filesDir, CROP_DIRECTORY).apply { mkdirs() }
    }

    /** Resolve crops for a batch of faces; misses decode each distinct media once. */
    suspend fun faceCrops(faces: List<DetectedFaceEntity>): List<Pair<Long, String>> {
        val mediaCache = HashMap<Long, UriMedia?>()
        return faces.mapNotNull { face ->
            val file = cropFile(face)
            if (!file.exists()) {
                val media = mediaCache.getOrPut(face.mediaId) { findMedia(face.mediaId) }
                    ?: return@mapNotNull null
                if (!writeCrop(face, media, file)) return@mapNotNull null
            }
            face.id to file.toUri().toString()
        }
    }

    private fun cropFile(face: DetectedFaceEntity): File =
        File(directory, "${face.id}_${face.resultRevision.hashCode()}.jpg")

    private suspend fun findMedia(mediaId: Long): UriMedia? =
        mediaRepository.getCompleteMedia().first().data?.firstOrNull { it.id == mediaId }
            ?: cloudMediaDao.getAllCachedAsync()
                .firstOrNull { it.globalMediaId == mediaId }
                ?.toUriMedia()

    private suspend fun writeCrop(
        face: DetectedFaceEntity,
        media: UriMedia,
        file: File
    ): Boolean = runCatching {
        val bitmap = thumbnailLoader.load(media, DECODE_SIZE) ?: return@runCatching false
        try {
            // Square crop centered on the face with breathing room — a tight box
            // crop reads as "too close" in the strip and circular avatars.
            val faceCx = (face.left + face.right) / 2f * bitmap.width
            val faceCy = (face.top + face.bottom) / 2f * bitmap.height
            val faceW = (face.right - face.left) * bitmap.width
            val faceH = (face.bottom - face.top) * bitmap.height
            val side = (maxOf(faceW, faceH) * CROP_MARGIN).toInt()
                .coerceIn(2, minOf(bitmap.width, bitmap.height))
            val left = (faceCx - side / 2f).toInt().coerceIn(0, bitmap.width - side)
            val top = (faceCy - side / 2f).toInt().coerceIn(0, bitmap.height - side)
            val crop = Bitmap.createBitmap(bitmap, left, top, side, side)
            file.outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (crop != bitmap) crop.recycle()
            true
        } finally {
            bitmap.recycle()
        }
    }.getOrDefault(false)

    private companion object {
        const val CROP_DIRECTORY = "face_thumbs/crops"
        const val DECODE_SIZE = 640
        /** Crop side = face box long edge × this factor — keeps head + some context. */
        const val CROP_MARGIN = 1.45f
    }
}
