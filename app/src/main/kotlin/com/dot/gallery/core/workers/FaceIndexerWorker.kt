/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.BuildConfig
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.DetectedFaceDao
import com.dot.gallery.cloud.data.dao.PersonDao
import com.dot.gallery.cloud.data.entity.DetectedFaceEntity
import com.dot.gallery.cloud.data.entity.PersonEntity
import com.dot.gallery.core.ml.DetectedFaceBox
import com.dot.gallery.core.ml.FaceHelper
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.presentation.util.printInfo
import com.dot.gallery.feature_node.presentation.util.printWarning
import com.github.panpf.sketch.asBitmapOrNull
import com.github.panpf.sketch.decode.BitmapColorSpace
import com.github.panpf.sketch.request.ImageRequest
import com.github.panpf.sketch.sketch
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Manually (re)start on-device face indexing — used by the "Scan for people" action in the
 * People screen. Enqueued as the same unique work as the always-on indexer so progress is
 * observable via [FaceIndexerWorker.WORK_NAME] and only one scan runs at a time.
 */
fun WorkManager.forceFaceIndex() {
    val constraints = Constraints.Builder()
        .setRequiresStorageNotLow(true)
        .build()
    val work = OneTimeWorkRequestBuilder<FaceIndexerWorker>()
        .setConstraints(constraints)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .addTag(FaceIndexerWorker.WORK_NAME)
        .build()
    enqueueUniqueWork(FaceIndexerWorker.WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, work)
}

/**
 * Always-on background indexer that powers on-device Person grouping. Runs whenever the
 * [ModelGroup.FACE_DETECT] model is installed: it detects faces in un-indexed local media,
 * embeds them (when [ModelGroup.FACE_RECOGNITION] is also installed) and clusters embeddings
 * into people using incremental cosine-centroid matching.
 */
@HiltWorker
class FaceIndexerWorker @AssistedInject constructor(
    private val repository: MediaRepository,
    private val modelManager: ModelManager,
    private val personDao: PersonDao,
    private val faceDao: DetectedFaceDao,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val faceHelper by lazy { FaceHelper(modelManager) }

    private class Cluster(
        val personId: String,
        val centroid: FloatArray,
        var count: Int
    )

    override suspend fun doWork(): Result = runCatching {
        setProgress(workDataOf(KEY_PROGRESS to -1f))
        if (!BuildConfig.ENABLE_INDEXING) return Result.success()
        if (!modelManager.isReady(ModelGroup.FACE_DETECT)) {
            printInfo("FaceIndexer: face detector not installed, skipping")
            return Result.success()
        }
        if (!currentCoroutineContext().isActive) return Result.success()

        val media = repository.getCompleteMedia().map { it.data ?: emptyList() }.firstOrNull().orEmpty()
        val indexed = faceDao.getIndexedMediaIds().toHashSet()
        val toIndex = media.filter { it.id !in indexed && it.mimeType.startsWith("image") }
        if (toIndex.isEmpty()) {
            printInfo("FaceIndexer: nothing to index")
            return Result.success()
        }

        // Seed clusters from previously assigned faces.
        val clusters = buildInitialClusters()
        val touchedPeople = HashSet<String>()

        setProgress(workDataOf(KEY_PROGRESS to 0f))
        val total = toIndex.size
        try {
            toIndex.forEachIndexed { index, item ->
                if (!currentCoroutineContext().isActive || isStopped) return@forEachIndexed
                setProgress(workDataOf(KEY_PROGRESS to (index.toFloat() / total) * 100f))

                val request = ImageRequest(appContext, item.getUri().toString()) {
                    colorSpace(BitmapColorSpace(ColorSpace.Named.SRGB))
                    size(640, 640)
                    setExtra("realMimeType", item.mimeType)
                }
                val bitmap = appContext.sketch.execute(request).image?.asBitmapOrNull()
                if (bitmap == null) {
                    // Record an empty marker so we don't re-scan endlessly.
                    faceDao.insert(DetectedFaceEntity(mediaId = item.id, timestamp = item.timestamp, confidence = 0f))
                    yield()
                    return@forEachIndexed
                }

                val faces = runCatching { faceHelper.detect(bitmap) }.getOrDefault(emptyList())
                if (faces.isEmpty()) {
                    faceDao.insert(DetectedFaceEntity(mediaId = item.id, timestamp = item.timestamp, confidence = 0f))
                } else {
                    for (face in faces) {
                        val embedding = faceHelper.embed(bitmap, face.rectF)
                        val personId = embedding?.let { assignCluster(it, clusters, item, face, bitmap, touchedPeople) }
                        faceDao.insert(
                            DetectedFaceEntity(
                                mediaId = item.id,
                                personId = personId,
                                embedding = embedding?.let { encode(it) },
                                left = face.left,
                                top = face.top,
                                right = face.right,
                                bottom = face.bottom,
                                confidence = face.confidence,
                                timestamp = item.timestamp
                            )
                        )
                    }
                }
                yield()
            }
        } finally {
            faceHelper.close()
        }

        // Refresh face counts for people we touched this run.
        val now = System.currentTimeMillis()
        touchedPeople.forEach { id ->
            personDao.updateFaceCount(id, faceDao.countForPerson(id), now)
        }

        setProgress(workDataOf(KEY_PROGRESS to 100f))
        printInfo("FaceIndexer: indexed $total media, ${clusters.size} people known")
        Result.success()
    }.getOrElse {
        printWarning("FaceIndexer failed: ${it.message}")
        Result.failure()
    }

    /** Load existing person centroids from stored face embeddings. */
    private suspend fun buildInitialClusters(): MutableList<Cluster> {
        val faces = faceDao.getAll().filter { it.personId != null && it.embedding != null }
        return faces.groupBy { it.personId!! }.map { (personId, group) ->
            val dim = decode(group.first().embedding!!).size
            val sum = FloatArray(dim)
            group.forEach { f ->
                val e = decode(f.embedding!!)
                for (i in 0 until dim) sum[i] += e[i]
            }
            for (i in 0 until dim) sum[i] /= group.size
            Cluster(personId, FaceHelper.l2Normalize(sum), group.size)
        }.toMutableList()
    }

    /**
     * Assign [embedding] to the nearest cluster above [CLUSTER_THRESHOLD], or create a new person.
     * Returns the resolved personId.
     */
    private suspend fun assignCluster(
        embedding: FloatArray,
        clusters: MutableList<Cluster>,
        item: com.dot.gallery.feature_node.domain.model.Media.UriMedia,
        face: DetectedFaceBox,
        bitmap: Bitmap,
        touched: HashSet<String>
    ): String {
        var best: Cluster? = null
        var bestSim = -1f
        for (c in clusters) {
            val sim = FaceHelper.cosine(embedding, c.centroid)
            if (sim > bestSim) {
                bestSim = sim
                best = c
            }
        }
        if (best != null && bestSim >= CLUSTER_THRESHOLD) {
            // Running-mean centroid update.
            val n = best.count
            for (i in best.centroid.indices) {
                best.centroid[i] = (best.centroid[i] * n + embedding[i]) / (n + 1)
            }
            val renorm = FaceHelper.l2Normalize(best.centroid)
            System.arraycopy(renorm, 0, best.centroid, 0, renorm.size)
            best.count = n + 1
            touched.add(best.personId)
            return best.personId
        }

        // New person.
        val personId = "local_${UUID.randomUUID()}"
        val thumbUrl = saveFaceThumbnail(personId, bitmap, face)
        personDao.insert(
            PersonEntity(
                id = personId,
                name = "",
                providerType = ProviderType.LOCAL_PEOPLE,
                thumbnailMediaId = item.id,
                thumbnailUrl = thumbUrl,
                faceCount = 1,
                lastUpdated = System.currentTimeMillis()
            )
        )
        clusters.add(Cluster(personId, embedding.copyOf(), 1))
        touched.add(personId)
        return personId
    }

    /** Crop the face box and persist it as this person's cover thumbnail. Returns a file uri. */
    private fun saveFaceThumbnail(personId: String, bitmap: Bitmap, face: DetectedFaceBox): String? = try {
        val left = (face.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (face.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (face.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (face.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val dir = File(appContext.filesDir, THUMB_DIR).apply { mkdirs() }
        val file = File(dir, "$personId.jpg")
        file.outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (crop != bitmap) crop.recycle()
        file.toUri().toString()
    } catch (e: Exception) {
        printWarning("FaceIndexer: thumbnail save failed: ${e.message}")
        null
    }

    companion object {
        const val KEY_PROGRESS = "progress"
        const val WORK_NAME = "FaceIndexer"
        const val THUMB_DIR = "face_thumbs"

        /** Cosine similarity above which two faces are treated as the same person. */
        const val CLUSTER_THRESHOLD = 0.45f

        fun encode(v: FloatArray): ByteArray {
            val buf = ByteBuffer.allocate(v.size * 4)
            v.forEach { buf.putFloat(it) }
            return buf.array()
        }

        fun decode(bytes: ByteArray): FloatArray {
            val buf = ByteBuffer.wrap(bytes)
            return FloatArray(bytes.size / 4) { buf.float }
        }
    }
}
