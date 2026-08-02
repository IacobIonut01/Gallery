/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.smart

import android.content.Context
import android.graphics.Bitmap
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.dot.gallery.BuildConfig
import com.dot.gallery.core.Resource
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.DetectedFaceDao
import com.dot.gallery.cloud.data.dao.PersonDao
import com.dot.gallery.cloud.data.entity.DetectedFaceEntity
import com.dot.gallery.cloud.data.entity.PersonEntity
import com.dot.gallery.core.ml.DetectedFaceBox
import com.dot.gallery.core.ml.FaceHelper
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.MediaFeature
import com.dot.gallery.feature_node.data.data_source.MediaFeatureStateEntity
import com.dot.gallery.feature_node.data.data_source.MediaFeatureStatus
import com.dot.gallery.feature_node.data.data_source.SmartScanPhase
import com.dot.gallery.feature_node.domain.model.Category
import com.dot.gallery.feature_node.domain.model.ImageEmbedding
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.MediaCategory
import com.dot.gallery.feature_node.domain.model.MediaVersion
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.search.helpers.SearchVisionHelper
import com.dot.gallery.feature_node.presentation.search.util.dot
import com.dot.gallery.feature_node.presentation.util.mediaStoreVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject

sealed interface SmartScanPhaseResult {
    val progress: SmartScanProgress

    data class Completed(
        override val progress: SmartScanProgress,
        val sourceSnapshot: String? = null
    ) : SmartScanPhaseResult

    data class Partial(
        val errorCode: String,
        override val progress: SmartScanProgress
    ) : SmartScanPhaseResult

    data class Blocked(
        val errorCode: String,
        override val progress: SmartScanProgress = SmartScanProgress.EMPTY
    ) : SmartScanPhaseResult

    data class Failed(
        val errorCode: String,
        val retryable: Boolean,
        override val progress: SmartScanProgress = SmartScanProgress.EMPTY
    ) : SmartScanPhaseResult
}

data class SmartScanPhaseContext(
    val runId: String,
    val sourceSnapshot: String,
    val fullRefresh: Boolean,
    val reportProgress: suspend (SmartScanProgress) -> Unit
)

interface SmartScanPhaseProcessor {
    val phase: SmartScanPhase
    val revision: String
    suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult
}

class SmartScanProcessorRegistry @Inject constructor(
    processors: Set<@JvmSuppressWildcards SmartScanPhaseProcessor>
) {
    private val processorsByPhase = processors.associateBy { it.phase }.also {
        require(it.size == processors.size) { "Only one Smart Scan processor may handle each phase" }
    }

    fun processorFor(phase: SmartScanPhase): SmartScanPhaseProcessor =
        requireNotNull(processorsByPhase[phase]) { "No Smart Scan processor registered for $phase" }
}

abstract class MediaPhaseProcessor(
    protected val repository: MediaRepository,
    protected val database: InternalDatabase
) : SmartScanPhaseProcessor {
    protected suspend fun localMedia(): List<Media.UriMedia> {
        val result = repository.getCompleteMedia().firstOrNull()
            ?: throw IllegalStateException("media_source_unavailable")
        if (result !is Resource.Success) throw IllegalStateException("media_source_failed")
        val media = result.data ?: throw IllegalStateException("media_source_missing")
        if (media.isEmpty() && database.getMediaDao().getMedia().isNotEmpty()) {
            throw IllegalStateException("media_source_incomplete")
        }
        return media
    }

    protected suspend fun media(): List<Media.UriMedia> =
        localMedia() + database.getCloudMediaDao().getAllCachedAsync().map { it.toUriMedia() }

    protected fun sourceRevision(media: Media): String =
        "${media.timestamp}:${media.size}:${media.mimeType}:${media.path}"

    protected suspend fun progress(
        context: SmartScanPhaseContext,
        total: Int,
        processed: Int,
        succeeded: Int,
        skipped: Int,
        failed: Int
    ): SmartScanProgress = SmartScanProgress(total, processed, succeeded, skipped, failed).also {
        context.reportProgress(it)
    }

    protected companion object {
        const val ITEM_LEASE_MILLIS = 90_000L
        const val ITEM_TIMEOUT_MILLIS = 60_000L
        const val RETRY_DELAY_MILLIS = 6 * 60 * 60 * 1000L
    }
}

class SourceSyncProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase,
    @ApplicationContext private val appContext: Context
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.SOURCE_SYNC
    override val revision = "source-v1"

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        val media = localMedia()
        val mediaStoreVersion = appContext.mediaStoreVersion
        database.withTransaction {
            database.getMediaDao().updateMedia(media)
            database.getMediaDao().setMediaVersion(MediaVersion(mediaStoreVersion))
        }
        val cloud = database.getCloudMediaDao().getAllCachedAsync()
        val snapshot = "$mediaStoreVersion:${media.size}:${cloud.size}:" +
            "${media.maxOfOrNull { it.timestamp } ?: 0L}:${cloud.maxOfOrNull { it.timestamp } ?: 0L}"
        val summary = progress(context, media.size + cloud.size, media.size + cloud.size, media.size + cloud.size, 0, 0)
        return SmartScanPhaseResult.Completed(summary, snapshot)
    }
}

class MetadataPhaseProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.METADATA
    override val revision = "metadata-v1"

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        val scanDao = database.getSmartScanDao()
        database.getMetadataDao().deleteOrphans()
        val now = System.currentTimeMillis()
        val candidates = media().filter { item ->
            val source = sourceRevision(item)
            val state = scanDao.getFeatureState(item.id, MediaFeature.METADATA)
            val isCurrent = state?.status == MediaFeatureStatus.SUCCEEDED &&
                state.sourceRevision == source && state.resultRevision == revision
            if (!SmartScanPlan.shouldProcess(context.fullRefresh, isCurrent)) false else {
                scanDao.upsertFeatureState(
                    MediaFeatureStateEntity(
                        mediaId = item.id,
                        feature = MediaFeature.METADATA,
                        sourceRevision = source,
                        updatedAt = now
                    )
                )
                true
            }
        }
        var succeeded = 0
        var skipped = 0
        var failed = 0
        candidates.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            val owner = "${context.runId}:metadata:${item.id}"
            val attemptAt = System.currentTimeMillis()
            if (scanDao.claimFeatureLease(
                    item.id,
                    MediaFeature.METADATA,
                    context.runId,
                    owner,
                    attemptAt,
                    attemptAt + ITEM_LEASE_MILLIS
                ) != 1
            ) {
                skipped++
                progress(context, candidates.size, index + 1, succeeded, skipped, failed)
                return@forEachIndexed
            }
            runCatching {
                withTimeout(ITEM_TIMEOUT_MILLIS) { repository.collectMetadataFor(item, bulk = true) }
            }
                .onSuccess {
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.METADATA,
                        owner,
                        MediaFeatureStatus.SUCCEEDED,
                        revision,
                        System.currentTimeMillis()
                    )
                    succeeded++
                }
                .onFailure {
                    if (it is CancellationException && it !is TimeoutCancellationException) throw it
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.METADATA,
                        owner,
                        MediaFeatureStatus.FAILED,
                        revision,
                        System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS,
                        lastErrorCode = "parse_failed"
                    )
                    failed++
                }
            progress(context, candidates.size, index + 1, succeeded, skipped, failed)
            yield()
        }
        val summary = SmartScanProgress(candidates.size, candidates.size, succeeded, skipped, failed)
        return when {
            failed == candidates.size && candidates.isNotEmpty() ->
                SmartScanPhaseResult.Failed("metadata_processing_failed", retryable = true, summary)
            failed > 0 -> SmartScanPhaseResult.Partial("metadata_partially_failed", summary)
            else -> SmartScanPhaseResult.Completed(summary)
        }
    }
}

class SearchIndexPhaseProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase,
    private val modelManager: ModelManager,
    private val thumbnailLoader: SmartThumbnailLoader
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.SEARCH_INDEX
    override val revision: String
        get() = "clip-v2:${modelManager.processorRevision(ModelGroup.SEARCH)}"

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        if (!BuildConfig.ENABLE_INDEXING) return SmartScanPhaseResult.Blocked("indexing_disabled")
        if (!modelManager.isReady(ModelGroup.SEARCH)) return SmartScanPhaseResult.Blocked("search_model_unavailable")
        val scanDao = database.getSmartScanDao()
        database.getImageEmbeddingDao().deleteOrphans()
        val candidates = media().filter { it.mimeType.startsWith("image/") }.filter { item ->
            val source = sourceRevision(item)
            val embedding = repository.getRecord(item.id)
            val state = scanDao.getFeatureState(item.id, MediaFeature.SEARCH_EMBEDDING)
            if (!context.fullRefresh && embedding?.date == item.timestamp && embedding.resultRevision == revision &&
                state?.status == MediaFeatureStatus.SUCCEEDED && state.sourceRevision == source
            ) false else {
                scanDao.upsertFeatureState(
                    MediaFeatureStateEntity(
                        mediaId = item.id,
                        feature = MediaFeature.SEARCH_EMBEDDING,
                        sourceRevision = source,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
        var succeeded = 0
        var skipped = 0
        var failed = 0
        val helper = SearchVisionHelper(modelManager)
        helper.setupVisionSession().use { session ->
            candidates.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.runId}:embedding:${item.id}"
                val attemptAt = System.currentTimeMillis()
                if (scanDao.claimFeatureLease(
                        item.id,
                        MediaFeature.SEARCH_EMBEDDING,
                        context.runId,
                        owner,
                        attemptAt,
                        attemptAt + ITEM_LEASE_MILLIS
                    ) != 1
                ) {
                    skipped++
                    progress(context, candidates.size, index + 1, succeeded, skipped, failed)
                    return@forEachIndexed
                }
                val result = runCatching {
                    withTimeout(ITEM_TIMEOUT_MILLIS) {
                    val bitmap = thumbnailLoader.load(item, 224) ?: error("decode_failed")
                    try {
                        repository.addImageEmbedding(
                            ImageEmbedding(
                                item.id,
                                item.timestamp,
                                helper.getImageEmbedding(session, bitmap),
                                resultRevision = revision
                            )
                        )
                    } finally {
                        bitmap.recycle()
                    }
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.SEARCH_EMBEDDING,
                        owner,
                        MediaFeatureStatus.SUCCEEDED,
                        revision,
                        System.currentTimeMillis()
                    )
                    }
                }
                if (result.isSuccess) succeeded++ else {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.SEARCH_EMBEDDING,
                        owner,
                        MediaFeatureStatus.FAILED,
                        revision,
                        System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS,
                        lastErrorCode = "decode_or_inference_failed"
                    )
                    failed++
                }
                progress(context, candidates.size, index + 1, succeeded, skipped, failed)
                yield()
            }
        }
        val summary = SmartScanProgress(candidates.size, candidates.size, succeeded, skipped, failed)
        return when {
            failed == candidates.size && candidates.isNotEmpty() ->
                SmartScanPhaseResult.Failed("search_index_failed", retryable = true, summary)
            failed > 0 -> SmartScanPhaseResult.Partial("search_index_partially_failed", summary)
            else -> SmartScanPhaseResult.Completed(summary)
        }
    }
}

class CategoryClassificationPhaseProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase,
    private val modelManager: ModelManager
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.CATEGORY_CLASSIFICATION
    override val revision: String
        get() = "categories-v2:${modelManager.processorRevision(ModelGroup.SEARCH)}"

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        if (!BuildConfig.ENABLE_INDEXING) return SmartScanPhaseResult.Blocked("indexing_disabled")
        if (!modelManager.isReady(ModelGroup.SEARCH)) return SmartScanPhaseResult.Blocked("search_model_unavailable")
        val categoryDao = database.getCategoryDao()
        categoryDao.cleanupCategoriesForDeletedMedia()
        var categories = categoryDao.getAllCategoriesAsync()
        if (categories.isEmpty()) {
            categoryDao.insertCategories(Category.DEFAULT_CATEGORIES)
            categories = categoryDao.getAllCategoriesAsync()
        }
        val embeddings = repository.getImageEmbeddings().firstOrNull().orEmpty()
        if (embeddings.isEmpty()) return SmartScanPhaseResult.Blocked("embeddings_unavailable")
        val scanDao = database.getSmartScanDao()
        val embeddingGeneration =
            "${embeddings.size}:${scanDao.getFeatureGeneration(MediaFeature.SEARCH_EMBEDDING)}:$revision"
        categories = categories.filter { category ->
            val source = "$embeddingGeneration:${category.updatedAt}:${category.searchTerms}:" +
                "${category.threshold}:${category.referenceImageIds.joinToString(",")}"
            val state = scanDao.getFeatureState(category.id, MediaFeature.CATEGORY_CLASSIFICATION)
            val isCurrent = state?.status == MediaFeatureStatus.SUCCEEDED &&
                state.sourceRevision == source && state.resultRevision == revision
            if (!SmartScanPlan.shouldProcess(context.fullRefresh, isCurrent)) false else {
                scanDao.upsertFeatureState(
                    MediaFeatureStateEntity(
                        mediaId = category.id,
                        feature = MediaFeature.CATEGORY_CLASSIFICATION,
                        sourceRevision = source,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
        val helper = SearchVisionHelper(modelManager)
        var succeeded = 0
        var skipped = 0
        var failed = 0
        helper.setupTextSession().use { session ->
            categories.forEachIndexed { index, category ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.runId}:category:${category.id}"
                val attemptAt = System.currentTimeMillis()
                if (scanDao.claimFeatureLease(
                        category.id,
                        MediaFeature.CATEGORY_CLASSIFICATION,
                        context.runId,
                        owner,
                        attemptAt,
                        attemptAt + ITEM_LEASE_MILLIS
                    ) != 1
                ) {
                    skipped++
                    progress(context, categories.size, index + 1, succeeded, skipped, failed)
                    return@forEachIndexed
                }
                val result = runCatching {
                    val categoryEmbedding = category.embedding ?: if (category.searchTerms.isNotBlank()) {
                        helper.getTextEmbedding(session, category.searchTerms).also {
                            categoryDao.updateCategory(category.copy(embedding = it, updatedAt = System.currentTimeMillis()))
                        }
                    } else null
                    val references = category.referenceImageIds.toSet()
                    val referenceEmbeddings = embeddings.filter { it.id in references }
                    if (categoryEmbedding == null && referenceEmbeddings.isEmpty()) {
                        scanDao.finishFeature(
                            category.id,
                            MediaFeature.CATEGORY_CLASSIFICATION,
                            owner,
                            MediaFeatureStatus.SKIPPED,
                            revision,
                            System.currentTimeMillis(),
                            lastErrorCode = "category_has_no_terms"
                        )
                        skipped++
                        return@runCatching
                    }
                    val matches = embeddings.mapNotNull { image ->
                        val textScore = categoryEmbedding?.let { image.embedding dot it } ?: Float.NEGATIVE_INFINITY
                        val referenceScore = referenceEmbeddings.maxOfOrNull { image.embedding dot it.embedding }
                            ?: Float.NEGATIVE_INFINITY
                        maxOf(textScore, referenceScore).takeIf { it >= category.threshold }?.let { score ->
                            MediaCategory(image.id, category.id, score, resultRevision = revision)
                        }
                    }
                    categoryDao.reclassifyMediaForCategory(category.id, matches)
                    scanDao.finishFeature(
                        category.id,
                        MediaFeature.CATEGORY_CLASSIFICATION,
                        owner,
                        MediaFeatureStatus.SUCCEEDED,
                        revision,
                        System.currentTimeMillis()
                    )
                    succeeded++
                }
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                    scanDao.finishFeature(
                        category.id,
                        MediaFeature.CATEGORY_CLASSIFICATION,
                        owner,
                        MediaFeatureStatus.FAILED,
                        revision,
                        System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS,
                        lastErrorCode = "classification_failed"
                    )
                    failed++
                }
                progress(context, categories.size, index + 1, succeeded, skipped, failed)
            }
        }
        val summary = SmartScanProgress(categories.size, categories.size, succeeded, skipped, failed)
        return when {
            failed == categories.size && categories.isNotEmpty() ->
                SmartScanPhaseResult.Failed("category_classification_failed", retryable = true, summary)
            failed > 0 -> SmartScanPhaseResult.Partial("category_classification_partially_failed", summary)
            else -> SmartScanPhaseResult.Completed(summary)
        }
    }
}

class FaceIndexPhaseProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase,
    private val modelManager: ModelManager,
    private val faceDao: DetectedFaceDao,
    private val personDao: PersonDao,
    private val thumbnailLoader: SmartThumbnailLoader,
    @ApplicationContext private val appContext: Context
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.FACE_INDEX
    override val revision: String
        get() = "face-v2:${modelManager.processorRevision(ModelGroup.FACE_DETECT)}:" +
            modelManager.processorRevision(ModelGroup.FACE_RECOGNITION)

    private data class Cluster(val personId: String, val centroid: FloatArray, var count: Int)

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        if (!BuildConfig.ENABLE_INDEXING) return SmartScanPhaseResult.Blocked("indexing_disabled")
        if (!modelManager.isReady(ModelGroup.FACE_DETECT) ||
            !modelManager.isReady(ModelGroup.FACE_RECOGNITION)
        ) return SmartScanPhaseResult.Blocked("face_model_unavailable")
        val scanDao = database.getSmartScanDao()
        faceDao.deleteOrphans()
        val candidates = media().filter { it.mimeType.startsWith("image/") }.filter { item ->
            val source = sourceRevision(item)
            val existing = faceDao.getByMedia(item.id)
            val state = scanDao.getFeatureState(item.id, MediaFeature.FACE_DETECTION)
            if (!context.fullRefresh && existing.isNotEmpty() && state == null && existing.all { it.resultRevision.isBlank() }) {
                scanDao.upsertFeatureState(
                    MediaFeatureStateEntity(
                        mediaId = item.id,
                        feature = MediaFeature.FACE_DETECTION,
                        status = MediaFeatureStatus.SUCCEEDED,
                        sourceRevision = source,
                        resultRevision = revision,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                false
            } else if (!context.fullRefresh && existing.isNotEmpty() &&
                existing.all { it.timestamp == item.timestamp && it.resultRevision == revision } &&
                state?.status == MediaFeatureStatus.SUCCEEDED && state.sourceRevision == source
            ) false else {
                scanDao.upsertFeatureState(
                    MediaFeatureStateEntity(
                        mediaId = item.id,
                        feature = MediaFeature.FACE_DETECTION,
                        sourceRevision = source,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
        val clusters = buildInitialClusters(candidates.mapTo(hashSetOf()) { it.id })
        val touchedPeople = hashSetOf<String>()
        var succeeded = 0
        var skipped = 0
        var failed = 0
        val helper = FaceHelper(modelManager)
        try {
            candidates.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.runId}:face:${item.id}"
                val attemptAt = System.currentTimeMillis()
                if (scanDao.claimFeatureLease(
                        item.id,
                        MediaFeature.FACE_DETECTION,
                        context.runId,
                        owner,
                        attemptAt,
                        attemptAt + ITEM_LEASE_MILLIS
                    ) != 1
                ) {
                    skipped++
                    progress(context, candidates.size, index + 1, succeeded, skipped, failed)
                    return@forEachIndexed
                }
                val result = runCatching {
                    val bitmap = thumbnailLoader.load(item, 640) ?: error("decode_failed")
                    try {
                        val faces = helper.detect(bitmap)
                        val embeddedFaces = faces.map { face ->
                            face to requireNotNull(helper.embed(bitmap, face.rectF))
                        }
                        faceDao.getByMedia(item.id).mapNotNullTo(touchedPeople) { it.personId }
                        faceDao.deleteByMedia(item.id)
                        if (embeddedFaces.isEmpty()) {
                            faceDao.insert(DetectedFaceEntity(mediaId = item.id, timestamp = item.timestamp, resultRevision = revision))
                        } else {
                            embeddedFaces.forEach { (face, embedding) ->
                                val personId = assignCluster(
                                    embedding,
                                    clusters,
                                    item,
                                    face,
                                    bitmap,
                                    touchedPeople
                                )
                                faceDao.insert(
                                    DetectedFaceEntity(
                                        mediaId = item.id,
                                        personId = personId,
                                        embedding = encode(embedding),
                                        left = face.left,
                                        top = face.top,
                                        right = face.right,
                                        bottom = face.bottom,
                                        confidence = face.confidence,
                                        timestamp = item.timestamp,
                                        resultRevision = revision
                                    )
                                )
                            }
                        }
                    } finally {
                        bitmap.recycle()
                    }
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.FACE_DETECTION,
                        owner,
                        MediaFeatureStatus.SUCCEEDED,
                        revision,
                        System.currentTimeMillis()
                    )
                }
                if (result.isSuccess) succeeded++ else {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException && error !is TimeoutCancellationException) throw error
                    scanDao.finishFeature(
                        item.id,
                        MediaFeature.FACE_DETECTION,
                        owner,
                        MediaFeatureStatus.FAILED,
                        revision,
                        System.currentTimeMillis(),
                        nextRetryAt = System.currentTimeMillis() + RETRY_DELAY_MILLIS,
                        lastErrorCode = "decode_or_inference_failed"
                    )
                    failed++
                }
                progress(context, candidates.size, index + 1, succeeded, skipped, failed)
                yield()
            }
        } finally {
            helper.close()
        }
        val now = System.currentTimeMillis()
        touchedPeople.forEach { personId ->
            personDao.updateFaceCount(personId, faceDao.countForPerson(personId), now)
        }
        val summary = SmartScanProgress(candidates.size, candidates.size, succeeded, skipped, failed)
        return when {
            failed == candidates.size && candidates.isNotEmpty() ->
                SmartScanPhaseResult.Failed("face_index_failed", retryable = true, summary)
            failed > 0 -> SmartScanPhaseResult.Partial("face_index_partially_failed", summary)
            else -> SmartScanPhaseResult.Completed(summary)
        }
    }

    private suspend fun buildInitialClusters(excludedMediaIds: Set<Long>): MutableList<Cluster> = faceDao.getAll()
        .filter { it.mediaId !in excludedMediaIds && it.personId != null && it.embedding != null }
        .groupBy { requireNotNull(it.personId) }
        .map { (personId, faces) ->
            val first = decode(requireNotNull(faces.first().embedding))
            val sum = FloatArray(first.size)
            faces.forEach { face ->
                val values = decode(requireNotNull(face.embedding))
                values.indices.forEach { index -> sum[index] += values[index] }
            }
            sum.indices.forEach { index -> sum[index] /= faces.size }
            Cluster(personId, FaceHelper.l2Normalize(sum), faces.size)
        }.toMutableList()

    private suspend fun assignCluster(
        embedding: FloatArray,
        clusters: MutableList<Cluster>,
        media: Media.UriMedia,
        face: DetectedFaceBox,
        bitmap: Bitmap,
        touchedPeople: MutableSet<String>
    ): String {
        val best = clusters.maxByOrNull { FaceHelper.cosine(embedding, it.centroid) }
        if (best != null && FaceHelper.cosine(embedding, best.centroid) >= CLUSTER_THRESHOLD) {
            best.centroid.indices.forEach { index ->
                best.centroid[index] =
                    (best.centroid[index] * best.count + embedding[index]) / (best.count + 1)
            }
            val normalized = FaceHelper.l2Normalize(best.centroid)
            System.arraycopy(normalized, 0, best.centroid, 0, normalized.size)
            best.count++
            touchedPeople += best.personId
            return best.personId
        }

        val personId = "local_${UUID.randomUUID()}"
        personDao.insert(
            PersonEntity(
                id = personId,
                name = "",
                providerType = ProviderType.LOCAL_PEOPLE,
                thumbnailMediaId = media.id,
                thumbnailUrl = saveFaceThumbnail(personId, bitmap, face),
                faceCount = 1,
                lastUpdated = System.currentTimeMillis()
            )
        )
        clusters += Cluster(personId, embedding.copyOf(), 1)
        touchedPeople += personId
        return personId
    }

    private fun saveFaceThumbnail(
        personId: String,
        bitmap: Bitmap,
        face: DetectedFaceBox
    ): String? = runCatching {
        val left = (face.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (face.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (face.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (face.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        val directory = File(appContext.filesDir, FACE_THUMBNAIL_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "$personId.jpg")
        file.outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        if (crop != bitmap) crop.recycle()
        file.toUri().toString()
    }.getOrNull()

    private fun encode(values: FloatArray): ByteArray = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).apply {
        values.forEach(::putFloat)
    }.array()

    private fun decode(bytes: ByteArray): FloatArray = ByteBuffer.wrap(bytes).let { buffer ->
        FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float }
    }

    companion object {
        private const val CLUSTER_THRESHOLD = 0.45f
        private const val FACE_THUMBNAIL_DIRECTORY = "face_thumbs"
    }
}
