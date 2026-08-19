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
import com.dot.gallery.core.Settings
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.data.dao.DetectedFaceDao
import com.dot.gallery.cloud.data.dao.DetectedFaceHeader
import com.dot.gallery.cloud.data.dao.PersonDao
import com.dot.gallery.cloud.data.entity.CloudMediaEntity
import com.dot.gallery.cloud.data.entity.DetectedFaceEntity
import com.dot.gallery.cloud.data.entity.FaceClusterEntity
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
import com.dot.gallery.feature_node.domain.util.FloatVectorCodec
import com.dot.gallery.feature_node.presentation.search.helpers.SearchVisionHelper
import com.dot.gallery.feature_node.presentation.search.util.dot
import com.dot.gallery.feature_node.presentation.util.mediaStoreVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.File
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
    val owner: String,
    val sourceSnapshot: String,
    val fullRefresh: Boolean,
    val reportProgress: suspend (SmartScanProgress) -> Unit
)

interface SmartScanPhaseProcessor {
    val phase: SmartScanPhase
    val revision: String
    suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult
}

internal data class FeatureWorkDecision(
    val shouldProcess: Boolean,
    val stateToPersist: MediaFeatureStateEntity? = null
)

internal fun prepareFeatureWork(
    existing: MediaFeatureStateEntity?,
    mediaId: Long,
    feature: MediaFeature,
    sourceRevision: String,
    resultRevision: String,
    fullRefresh: Boolean,
    now: Long,
    terminalOutputPresent: Boolean = true
): FeatureWorkDecision {
    if (existing == null) {
        return FeatureWorkDecision(
            shouldProcess = true,
            stateToPersist = MediaFeatureStateEntity(
                mediaId = mediaId,
                feature = feature,
                sourceRevision = sourceRevision,
                updatedAt = now
            )
        )
    }
    if (existing.status == MediaFeatureStatus.PROCESSING &&
        existing.leaseExpiresAt?.let { it > now } == true
    ) return FeatureWorkDecision(false)

    val sameSource = existing.sourceRevision == sourceRevision
    val sameResult = existing.resultRevision == resultRevision
    if (!fullRefresh && sameSource) {
        when (existing.status) {
            MediaFeatureStatus.PENDING -> return FeatureWorkDecision(true)
            MediaFeatureStatus.FAILED -> if (sameResult) {
                return FeatureWorkDecision(existing.nextRetryAt?.let { it <= now } != false)
            }
            MediaFeatureStatus.SUCCEEDED,
            MediaFeatureStatus.SKIPPED,
            MediaFeatureStatus.BLOCKED -> if (sameResult && terminalOutputPresent) return FeatureWorkDecision(false)
            MediaFeatureStatus.PROCESSING -> Unit
        }
    }

    return FeatureWorkDecision(
        shouldProcess = true,
        stateToPersist = existing.copy(
            status = MediaFeatureStatus.PENDING,
            sourceRevision = sourceRevision,
            resultRevision = "",
            attemptCount = if (sameSource && !fullRefresh) existing.attemptCount else 0,
            updatedAt = now,
            lastAttemptAt = if (sameSource && !fullRefresh) existing.lastAttemptAt else null,
            nextRetryAt = null,
            leaseOwner = null,
            leaseExpiresAt = null,
            runId = null,
            lastErrorCode = null
        )
    )
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
        database.getMediaDao().getMedia() + database.getCloudMediaDao().getAllCachedAsync().map { it.toUriMedia() }

    protected fun sourceRevision(media: Media): String = smartMediaSourceRevision(media)

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

internal fun <T> smartFeatureMediaPool(
    media: List<T>,
    includeIgnoredAlbums: Boolean,
    isIgnored: (T) -> Boolean,
    isLocked: (T) -> Boolean
): List<T> = media.filterNot { isLocked(it) || !includeIgnoredAlbums && isIgnored(it) }

internal fun shouldRefreshSmartLocalSource(fullRefresh: Boolean, mediaVersionCurrent: Boolean): Boolean =
    fullRefresh || !mediaVersionCurrent

internal fun smartMediaSourceRevision(media: Media): String =
    "${media.timestamp}:${media.size}:${media.mimeType}:${media.path}"

internal fun smartCloudSourceRevision(media: CloudMediaEntity): String {
    val displayName = media.providerType.displayName
    val displayPath = if (media.path.isNotBlank()) "$displayName/${media.path}" else "$displayName/${media.label}"
    return "${media.timestamp / 1000L}:${media.size}:${media.mimeType}:$displayPath"
}

internal fun smartSourceSnapshot(
    mediaStoreVersion: String,
    media: List<Media.UriMedia>,
    cloud: List<CloudMediaEntity>
): String {
    val cloudFingerprint = smartSourceFingerprint(cloud.map {
        "${it.providerType.name}/${it.serverConfigId}/${it.remoteId}:${smartCloudSourceRevision(it)}"
    })
    return "$mediaStoreVersion:${media.size}:${cloud.size}:" +
        "${media.maxOfOrNull { it.timestamp } ?: 0L}:${cloud.maxOfOrNull { it.timestamp } ?: 0L}:$cloudFingerprint"
}

class SourceSyncProcessor @Inject constructor(
    repository: MediaRepository,
    database: InternalDatabase,
    @ApplicationContext private val appContext: Context
) : MediaPhaseProcessor(repository, database) {
    override val phase = SmartScanPhase.SOURCE_SYNC
    override val revision = "source-v2"

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        val mediaStoreVersion = appContext.mediaStoreVersion
        val mediaDao = database.getMediaDao()
        val existingMedia = mediaDao.getMedia()
        val mediaVersionCurrent = mediaDao.isMediaVersionUpToDate(mediaStoreVersion)
        val localSource = if (shouldRefreshSmartLocalSource(context.fullRefresh, mediaVersionCurrent)) {
            localMedia()
        } else {
            existingMedia
        }
        val ignoredAlbums = repository.getBlacklistedAlbumsAsync()
        val lockedAlbumIds = repository.getLockedAlbums().first().mapTo(hashSetOf()) { it.id }
        val media = smartFeatureMediaPool(
            media = localSource,
            includeIgnoredAlbums = Settings.SmartFeatures.includeIgnoredAlbums(appContext).first(),
            isIgnored = { item -> ignoredAlbums.any { it.matchesMedia(item) } },
            isLocked = { item -> item.albumID in lockedAlbumIds }
        )
        val cloud = database.getCloudMediaDao().getAllCachedAsync()
        val existing = existingMedia.associateBy { it.id }
        val changed = media.filter { existing[it.id] != it }
        val currentIds = media.mapTo(hashSetOf()) { it.id }
        val removedIds = existing.keys.filterNot(currentIds::contains)
        val total = changed.size + removedIds.size
        progress(context, total, 0, 0, 0, 0)
        database.withTransaction {
            if (changed.isNotEmpty()) mediaDao.addMediaList(changed)
            removedIds.chunked(MEDIA_DELETE_BATCH_SIZE).forEach { mediaDao.deleteMediaByIds(it) }
            mediaDao.setMediaVersion(MediaVersion(mediaStoreVersion))
        }
        val snapshot = smartSourceSnapshot(mediaStoreVersion, media, cloud)
        val summary = progress(context, total, total, total, 0, 0)
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
        val metadataDao = database.getMetadataDao()
        metadataDao.deleteOrphans()
        val now = System.currentTimeMillis()
        val states = scanDao.getFeatureStates(MediaFeature.METADATA).associateBy { it.mediaId }
        val completeIds = if (context.fullRefresh) emptySet() else metadataDao.getCompleteMetadataIds().toHashSet()
        val statesToPersist = mutableListOf<MediaFeatureStateEntity>()
        val candidates = media().filter { item ->
            val source = sourceRevision(item)
            val state = states[item.id]
            val activeLease = state?.status == MediaFeatureStatus.PROCESSING &&
                state.leaseExpiresAt?.let { it > now } == true
            val current = !context.fullRefresh && item.id in completeIds &&
                state?.status == MediaFeatureStatus.SUCCEEDED && state.sourceRevision == source &&
                state.resultRevision == revision
            if (activeLease || current) {
                false
            } else if (item.id in completeIds && state?.sourceRevision == source) {
                statesToPersist += state.copy(
                    status = MediaFeatureStatus.SUCCEEDED,
                    sourceRevision = source,
                    resultRevision = revision,
                    updatedAt = now,
                    nextRetryAt = null,
                    leaseOwner = null,
                    leaseExpiresAt = null,
                    runId = null,
                    lastErrorCode = null
                )
                false
            } else {
                val decision = prepareFeatureWork(
                    state,
                    item.id,
                    MediaFeature.METADATA,
                    source,
                    revision,
                    context.fullRefresh,
                    now,
                    terminalOutputPresent = item.id in completeIds
                )
                decision.stateToPersist?.let(statesToPersist::add)
                decision.shouldProcess
            }
        }
        if (statesToPersist.isNotEmpty()) scanDao.upsertFeatureStates(statesToPersist)
        var succeeded = 0
        var skipped = 0
        var failed = 0
        progress(context, candidates.size, 0, 0, 0, 0)
        candidates.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            val owner = "${context.owner}:metadata:${item.id}"
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

private const val MEDIA_DELETE_BATCH_SIZE = 500
private const val PREPARATION_BATCH_SIZE = 500
private const val SEARCH_EMBEDDING_DIMENSION = 512
private const val FACE_EMBEDDING_DIMENSION = 512

internal fun smartSourceFingerprint(revisions: Iterable<String>): String {
    var hash = -3750763034362895579L
    revisions.sorted().forEach { revision ->
        revision.forEach { value -> hash = (hash xor value.code.toLong()) * 1099511628211L }
        hash = (hash xor 0xffffL) * 1099511628211L
    }
    return hash.toULong().toString(16)
}

internal fun isValidEmbeddingVector(values: FloatArray, expectedSize: Int): Boolean {
    if (values.size != expectedSize || values.any { !it.isFinite() }) return false
    val normSquared = values.fold(0.0) { sum, value -> sum + value * value }
    return normSquared in 0.9..1.1
}

internal fun canAdoptExistingSearchEmbedding(
    embedding: ImageEmbedding?,
    timestamp: Long,
    revision: String
): Boolean = embedding != null && embedding.date == timestamp &&
    (embedding.resultRevision.isBlank() || embedding.resultRevision == revision) &&
    isValidEmbeddingVector(embedding.embedding, SEARCH_EMBEDDING_DIMENSION)

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
        val imageEmbeddingDao = database.getImageEmbeddingDao()
        imageEmbeddingDao.deleteOrphans()
        val now = System.currentTimeMillis()
        val states = scanDao.getFeatureStates(MediaFeature.SEARCH_EMBEDDING).associateBy { it.mediaId }
        val headers = imageEmbeddingDao.getHeaders().associateBy { it.id }
        val statesToPersist = mutableListOf<MediaFeatureStateEntity>()
        val adoptedIds = mutableListOf<Long>()
        val candidates = mutableListOf<Media.UriMedia>()
        val searchMedia = media().filter { it.mimeType.startsWith("image/") }
        searchMedia.chunked(PREPARATION_BATCH_SIZE).forEach { batch ->
            val adoptionIds = if (context.fullRefresh) emptyList() else batch.mapNotNull { item ->
                val source = sourceRevision(item)
                val state = states[item.id]
                val header = headers[item.id]
                val outputPresent = header?.embeddingBytes == SEARCH_EMBEDDING_DIMENSION * Float.SIZE_BYTES
                val activeLease = state?.status == MediaFeatureStatus.PROCESSING &&
                    state.leaseExpiresAt?.let { it > now } == true
                val current = outputPresent && header.date == item.timestamp &&
                    header.resultRevision == revision && state?.status == MediaFeatureStatus.SUCCEEDED &&
                    state.sourceRevision == source && state.resultRevision == revision
                item.id.takeIf { !activeLease && !current && header?.date == item.timestamp }
            }
            val adoptionRecords = if (adoptionIds.isEmpty()) emptyMap()
            else imageEmbeddingDao.getRecords(adoptionIds).associateBy { it.id }
            batch.forEach { item ->
                val source = sourceRevision(item)
                val state = states[item.id]
                val header = headers[item.id]
                val activeLease = state?.status == MediaFeatureStatus.PROCESSING &&
                    state.leaseExpiresAt?.let { it > now } == true
                val outputPresent = header?.embeddingBytes == SEARCH_EMBEDDING_DIMENSION * Float.SIZE_BYTES
                val current = !context.fullRefresh && outputPresent && header.date == item.timestamp &&
                    header.resultRevision == revision && state?.status == MediaFeatureStatus.SUCCEEDED &&
                    state.sourceRevision == source && state.resultRevision == revision
                if (activeLease || current) return@forEach
                if (!context.fullRefresh && header?.date == item.timestamp &&
                    canAdoptExistingSearchEmbedding(adoptionRecords[item.id], item.timestamp, revision)
                ) {
                    adoptedIds += item.id
                    statesToPersist += state?.copy(
                        status = MediaFeatureStatus.SUCCEEDED,
                        sourceRevision = source,
                        resultRevision = revision,
                        updatedAt = now,
                        nextRetryAt = null,
                        leaseOwner = null,
                        leaseExpiresAt = null,
                        runId = null,
                        lastErrorCode = null
                    ) ?: MediaFeatureStateEntity(
                        mediaId = item.id,
                        feature = MediaFeature.SEARCH_EMBEDDING,
                        status = MediaFeatureStatus.SUCCEEDED,
                        sourceRevision = source,
                        resultRevision = revision,
                        updatedAt = now
                    )
                } else {
                    val decision = prepareFeatureWork(
                        state,
                        item.id,
                        MediaFeature.SEARCH_EMBEDDING,
                        source,
                        revision,
                        context.fullRefresh,
                        now,
                        terminalOutputPresent = outputPresent
                    )
                    decision.stateToPersist?.let(statesToPersist::add)
                    if (decision.shouldProcess) candidates += item
                }
            }
        }
        database.withTransaction {
            adoptedIds.chunked(PREPARATION_BATCH_SIZE).forEach { ids ->
                check(imageEmbeddingDao.updateResultRevisions(ids, revision) == ids.size)
            }
            if (statesToPersist.isNotEmpty()) scanDao.upsertFeatureStates(statesToPersist)
        }
        var succeeded = 0
        var skipped = 0
        var failed = 0
        progress(context, candidates.size, 0, 0, 0, 0)
        if (candidates.isEmpty()) return SmartScanPhaseResult.Completed(SmartScanProgress.EMPTY)
        val helper = SearchVisionHelper(modelManager)
        helper.setupVisionSession().use { session ->
            candidates.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.owner}:embedding:${item.id}"
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
                        val embedding = helper.getImageEmbedding(session, bitmap)
                        check(isValidEmbeddingVector(embedding, SEARCH_EMBEDDING_DIMENSION))
                        repository.addImageEmbedding(
                            ImageEmbedding(
                                item.id,
                                item.timestamp,
                                embedding,
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

internal fun canAdoptExistingCategoryResults(
    mappings: List<MediaCategory>,
    validMediaIds: Set<Long>,
    revision: String
): Boolean = mappings.isNotEmpty() && mappings.all {
    it.mediaId in validMediaIds && it.similarityScore.isFinite() &&
        (it.resultRevision.isBlank() || it.resultRevision == revision)
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
        val imageEmbeddingDao = database.getImageEmbeddingDao()
        val embeddingIds = imageEmbeddingDao.getIds().toHashSet()
        if (embeddingIds.isEmpty()) return SmartScanPhaseResult.Blocked("embeddings_unavailable")
        val scanDao = database.getSmartScanDao()
        val states = scanDao.getFeatureStates(MediaFeature.CATEGORY_CLASSIFICATION).associateBy { it.mediaId }
        val mappingsByCategory = categoryDao.getAllAutomaticMediaCategories().groupBy { it.categoryId }
        val embeddingGeneration =
            "${embeddingIds.size}:${scanDao.getFeatureGeneration(MediaFeature.SEARCH_EMBEDDING)}:$revision"
        val now = System.currentTimeMillis()
        val statesToPersist = mutableListOf<MediaFeatureStateEntity>()
        categories = categories.filter { category ->
            val source = "$embeddingGeneration:${category.updatedAt}:${category.searchTerms}:" +
                "${category.threshold}:${category.referenceImageIds.joinToString(",")}"
            val state = states[category.id]
            val activeLease = state?.status == MediaFeatureStatus.PROCESSING &&
                state.leaseExpiresAt?.let { it > now } == true
            val current = !context.fullRefresh && state?.status == MediaFeatureStatus.SUCCEEDED &&
                state.sourceRevision == source && state.resultRevision == revision
            val mappings = mappingsByCategory[category.id].orEmpty()
            if (activeLease || current) {
                false
            } else {
                if (!context.fullRefresh && canAdoptExistingCategoryResults(mappings, embeddingIds, revision)) {
                    database.withTransaction {
                        check(categoryDao.updateAutomaticResultRevision(category.id, revision) == mappings.size)
                        scanDao.upsertFeatureState(
                            state?.copy(
                                status = MediaFeatureStatus.SUCCEEDED,
                                sourceRevision = source,
                                resultRevision = revision,
                                updatedAt = now,
                                nextRetryAt = null,
                                leaseOwner = null,
                                leaseExpiresAt = null,
                                runId = null,
                                lastErrorCode = null
                            ) ?: MediaFeatureStateEntity(
                                mediaId = category.id,
                                feature = MediaFeature.CATEGORY_CLASSIFICATION,
                                status = MediaFeatureStatus.SUCCEEDED,
                                sourceRevision = source,
                                resultRevision = revision,
                                updatedAt = now
                            )
                        )
                    }
                    false
                } else {
                    val decision = prepareFeatureWork(
                        state,
                        category.id,
                        MediaFeature.CATEGORY_CLASSIFICATION,
                        source,
                        revision,
                        context.fullRefresh,
                        now
                    )
                    decision.stateToPersist?.let(statesToPersist::add)
                    decision.shouldProcess
                }
            }
        }
        if (statesToPersist.isNotEmpty()) scanDao.upsertFeatureStates(statesToPersist)
        if (categories.isEmpty()) return SmartScanPhaseResult.Completed(SmartScanProgress.EMPTY)
        val embeddings = repository.getImageEmbeddings().firstOrNull().orEmpty()
        if (embeddings.isEmpty()) return SmartScanPhaseResult.Blocked("embeddings_unavailable")
        val embeddingById = embeddings.associateBy { it.id }
        val helper = SearchVisionHelper(modelManager)
        var succeeded = 0
        var skipped = 0
        var failed = 0
        progress(context, categories.size, 0, 0, 0, 0)
        helper.setupTextSession().use { session ->
            categories.forEachIndexed { index, category ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.owner}:category:${category.id}"
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
                    val cachedEmbedding = category.embedding?.takeIf {
                        states[category.id]?.resultRevision == revision &&
                            isValidEmbeddingVector(it, SEARCH_EMBEDDING_DIMENSION)
                    }
                    val categoryEmbedding = cachedEmbedding ?: if (category.searchTerms.isNotBlank()) {
                        helper.getTextEmbedding(session, category.searchTerms).also {
                            categoryDao.updateCategoryEmbedding(category.id, it)
                        }
                    } else null
                    val referenceEmbeddings = category.referenceImageIds.mapNotNull(embeddingById::get)
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

internal fun canAdoptExistingFaceResults(
    faces: List<DetectedFaceEntity>,
    timestamp: Long,
    revision: String
): Boolean = faces.isNotEmpty() && faces.all { face ->
    if (face.timestamp != timestamp ||
        face.resultRevision.isNotBlank() && face.resultRevision != revision
    ) return@all false
    val values = floatArrayOf(face.left, face.top, face.right, face.bottom, face.confidence)
    if (values.any { !it.isFinite() }) return@all false
    val validEmbedding = face.embedding?.takeIf { it.size == FACE_EMBEDDING_DIMENSION * Float.SIZE_BYTES }
        ?.let { bytes ->
            isValidEmbeddingVector(FloatVectorCodec.decode(bytes), FACE_EMBEDDING_DIMENSION)
        } ?: false
    face.right > face.left && face.bottom > face.top && validEmbedding
}

internal fun isCurrentFaceDetection(
    state: MediaFeatureStateEntity?,
    sourceRevision: String,
    resultRevision: String,
    timestamp: Long,
    headers: List<DetectedFaceHeader>
): Boolean = state?.status == MediaFeatureStatus.SUCCEEDED &&
    state.sourceRevision == sourceRevision && state.resultRevision == resultRevision &&
    (headers.isEmpty() || headers.all { it.timestamp == timestamp && it.resultRevision == resultRevision })

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

    private data class Cluster(
        val personId: String,
        var centroid: FloatArray,
        var normalizedCentroid: FloatArray,
        var count: Int
    )

    override suspend fun process(context: SmartScanPhaseContext): SmartScanPhaseResult {
        if (!BuildConfig.ENABLE_INDEXING) return SmartScanPhaseResult.Blocked("indexing_disabled")
        if (!modelManager.isReady(ModelGroup.FACE_DETECT) ||
            !modelManager.isReady(ModelGroup.FACE_RECOGNITION)
        ) return SmartScanPhaseResult.Blocked("face_model_unavailable")
        val scanDao = database.getSmartScanDao()
        val orphanPeople = faceDao.getOrphanPersonIds()
        val removedOrphans = faceDao.deleteOrphans()
        val touchedPeople = orphanPeople.toHashSet()
        val now = System.currentTimeMillis()
        val states = scanDao.getFeatureStates(MediaFeature.FACE_DETECTION).associateBy { it.mediaId }
        val headers = faceDao.getHeaders().groupBy { it.mediaId }
        val statesToPersist = mutableListOf<MediaFeatureStateEntity>()
        val candidates = media().filter { it.mimeType.startsWith("image/") }.filter { item ->
            val source = sourceRevision(item)
            val state = states[item.id]
            val existingHeaders = headers[item.id].orEmpty()
            val activeLease = state?.status == MediaFeatureStatus.PROCESSING &&
                state.leaseExpiresAt?.let { it > now } == true
            val current = !context.fullRefresh && isCurrentFaceDetection(
                state = state,
                sourceRevision = source,
                resultRevision = revision,
                timestamp = item.timestamp,
                headers = existingHeaders
            )
            if (activeLease || current) {
                false
            } else {
                val existing = if (!context.fullRefresh && existingHeaders.isNotEmpty()) {
                    faceDao.getByMedia(item.id)
                } else {
                    emptyList()
                }
                if (!context.fullRefresh && canAdoptExistingFaceResults(existing, item.timestamp, revision)) {
                    existing.mapNotNullTo(touchedPeople) { it.personId }
                    database.withTransaction {
                        check(faceDao.updateResultRevision(item.id, revision) == existing.size)
                        scanDao.upsertFeatureState(
                            state?.copy(
                                status = MediaFeatureStatus.SUCCEEDED,
                                sourceRevision = source,
                                resultRevision = revision,
                                updatedAt = now,
                                nextRetryAt = null,
                                leaseOwner = null,
                                leaseExpiresAt = null,
                                runId = null,
                                lastErrorCode = null
                            ) ?: MediaFeatureStateEntity(
                                mediaId = item.id,
                                feature = MediaFeature.FACE_DETECTION,
                                status = MediaFeatureStatus.SUCCEEDED,
                                sourceRevision = source,
                                resultRevision = revision,
                                updatedAt = now
                            )
                        )
                    }
                    false
                } else {
                    val decision = prepareFeatureWork(
                        state,
                        item.id,
                        MediaFeature.FACE_DETECTION,
                        source,
                        revision,
                        context.fullRefresh,
                        now,
                        terminalOutputPresent = existingHeaders.isNotEmpty()
                    )
                    decision.stateToPersist?.let(statesToPersist::add)
                    decision.shouldProcess
                }
            }
        }
        if (statesToPersist.isNotEmpty()) scanDao.upsertFeatureStates(statesToPersist)
        if (candidates.isEmpty()) {
            if (removedOrphans > 0) persistClusters(buildInitialClusters(forceRebuild = true), touchedPeople)
            return SmartScanPhaseResult.Completed(SmartScanProgress.EMPTY)
        }
        val clusters = buildInitialClusters(forceRebuild = removedOrphans > 0)
        var succeeded = 0
        var skipped = 0
        var failed = 0
        progress(context, candidates.size, 0, 0, 0, 0)
        val helper = FaceHelper(modelManager)
        try {
            candidates.forEachIndexed { index, item ->
                currentCoroutineContext().ensureActive()
                val owner = "${context.owner}:face:${item.id}"
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
                val clusterSnapshot = clusters.mapTo(mutableListOf()) {
                    it.copy(centroid = it.centroid.copyOf(), normalizedCentroid = it.normalizedCentroid.copyOf())
                }
                val result = runCatching {
                    val bitmap = thumbnailLoader.load(item, 640) ?: error("decode_failed")
                    try {
                        val faces = helper.detect(bitmap)
                        val embeddedFaces = faces.map { face ->
                            val embedding = requireNotNull(helper.embed(bitmap, face.rectF))
                            check(isValidEmbeddingVector(embedding, FACE_EMBEDDING_DIMENSION))
                            face to embedding
                        }
                        val existingFaces = faceDao.getByMedia(item.id)
                        existingFaces.mapNotNullTo(touchedPeople) { it.personId }
                        removeFromClusters(existingFaces, clusters)
                        val detected = if (embeddedFaces.isEmpty()) {
                            listOf(DetectedFaceEntity(mediaId = item.id, timestamp = item.timestamp, resultRevision = revision))
                        } else {
                            embeddedFaces.map { (face, embedding) ->
                                DetectedFaceEntity(
                                    mediaId = item.id,
                                    personId = assignCluster(
                                        embedding,
                                        clusters,
                                        item,
                                        face,
                                        bitmap,
                                        touchedPeople
                                    ),
                                    embedding = FloatVectorCodec.encode(embedding),
                                    left = face.left,
                                    top = face.top,
                                    right = face.right,
                                    bottom = face.bottom,
                                    confidence = face.confidence,
                                    timestamp = item.timestamp,
                                    resultRevision = revision
                                )
                            }
                        }
                        val completedAt = System.currentTimeMillis()
                        database.withTransaction {
                            faceDao.deleteByMedia(item.id)
                            faceDao.insertAll(detected)
                            persistClusterRows(clusters, touchedPeople, completedAt)
                            val itemPeople = existingFaces.mapNotNullTo(hashSetOf()) { it.personId }
                            detected.mapNotNullTo(itemPeople) { it.personId }
                            itemPeople.forEach { personId ->
                                personDao.updateFaceCount(personId, faceDao.countForPerson(personId), completedAt)
                            }
                            check(
                                scanDao.finishFeature(
                                    item.id,
                                    MediaFeature.FACE_DETECTION,
                                    owner,
                                    MediaFeatureStatus.SUCCEEDED,
                                    revision,
                                    completedAt
                                ) == 1
                            )
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
                if (result.isSuccess) succeeded++ else {
                    val previousPeople = clusterSnapshot.mapTo(hashSetOf()) { it.personId }
                    val newPeople = clusters.mapNotNullTo(hashSetOf()) { it.personId.takeIf { id -> id !in previousPeople } }
                    withContext(NonCancellable) { deletePeople(newPeople) }
                    clusters.clear()
                    clusters.addAll(clusterSnapshot)
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
        persistClusters(clusters, touchedPeople)
        val summary = SmartScanProgress(candidates.size, candidates.size, succeeded, skipped, failed)
        return when {
            failed == candidates.size && candidates.isNotEmpty() ->
                SmartScanPhaseResult.Failed("face_index_failed", retryable = true, summary)
            failed > 0 -> SmartScanPhaseResult.Partial("face_index_partially_failed", summary)
            else -> SmartScanPhaseResult.Completed(summary)
        }
    }

    private suspend fun deletePeople(personIds: Set<String>) {
        personIds.forEach { personId ->
            personDao.getById(personId)?.thumbnailUrl?.let { value ->
                runCatching { File(requireNotNull(value.toUri().path)).delete() }
            }
            personDao.deleteById(personId)
        }
    }

    private suspend fun persistClusters(clusters: List<Cluster>, touchedPeople: Set<String>) {
        val completedAt = System.currentTimeMillis()
        database.withTransaction {
            persistClusterRows(clusters, touchedPeople, completedAt)
        }
        touchedPeople.forEach { personId ->
            personDao.updateFaceCount(personId, faceDao.countForPerson(personId), completedAt)
        }
    }

    private suspend fun persistClusterRows(
        clusters: List<Cluster>,
        touchedPeople: Set<String>,
        updatedAt: Long
    ) {
        val activeClusters = clusters.filter { it.count > 0 }.map {
            FaceClusterEntity(it.personId, it.centroid, it.count, updatedAt)
        }
        if (activeClusters.isNotEmpty()) faceDao.upsertClusters(activeClusters)
        val emptyClusterIds = touchedPeople - activeClusters.mapTo(hashSetOf()) { it.personId }
        if (emptyClusterIds.isNotEmpty()) faceDao.deleteClusters(emptyClusterIds.toList())
    }

    private suspend fun buildInitialClusters(forceRebuild: Boolean): MutableList<Cluster> {
        val persisted = faceDao.getClusters().filter {
            it.faceCount > 0 && it.centroid.size == FACE_EMBEDDING_DIMENSION && it.centroid.all(Float::isFinite)
        }
        val personCounts = faceDao.getPersonCounts().associate { it.personId to it.faceCount }
        val persistedCountsMatch = !forceRebuild && persisted.size == personCounts.size && persisted.all {
            personCounts[it.personId] == it.faceCount
        }
        if (persistedCountsMatch && persisted.isNotEmpty()) {
            return persisted.mapTo(mutableListOf()) {
                Cluster(it.personId, it.centroid.copyOf(), FaceHelper.l2Normalize(it.centroid), it.faceCount)
            }
        }
        val rebuilt = faceDao.getAll()
            .filter { it.personId != null && it.embedding != null }
            .groupBy { requireNotNull(it.personId) }
            .mapNotNull { (personId, faces) ->
                val vectors = faces.mapNotNull { face ->
                    runCatching { FloatVectorCodec.decode(requireNotNull(face.embedding)) }.getOrNull()
                        ?.takeIf { it.size == FACE_EMBEDDING_DIMENSION && it.all(Float::isFinite) }
                }
                if (vectors.isEmpty()) return@mapNotNull null
                val sum = FloatArray(FACE_EMBEDDING_DIMENSION)
                vectors.forEach { values ->
                    values.indices.forEach { index -> sum[index] += values[index] }
                }
                val centroid = FloatArray(sum.size) { sum[it] / vectors.size }
                Cluster(personId, centroid, FaceHelper.l2Normalize(centroid), vectors.size)
            }.toMutableList()
        if (rebuilt.isNotEmpty()) {
            val now = System.currentTimeMillis()
            faceDao.upsertClusters(rebuilt.map { FaceClusterEntity(it.personId, it.centroid, it.count, now) })
        }
        return rebuilt
    }

    private fun removeFromClusters(faces: List<DetectedFaceEntity>, clusters: List<Cluster>) {
        faces.forEach { face ->
            val personId = face.personId ?: return@forEach
            val embedding = face.embedding?.let {
                runCatching { FloatVectorCodec.decode(it) }.getOrNull()
            }?.takeIf { it.size == FACE_EMBEDDING_DIMENSION && it.all(Float::isFinite) } ?: return@forEach
            val cluster = clusters.firstOrNull { it.personId == personId } ?: return@forEach
            if (cluster.count > 1) {
                val newCount = cluster.count - 1
                cluster.centroid = FloatArray(cluster.centroid.size) { index ->
                    (cluster.centroid[index] * cluster.count - embedding[index]) / newCount
                }
                cluster.normalizedCentroid = FaceHelper.l2Normalize(cluster.centroid)
            }
            cluster.count = (cluster.count - 1).coerceAtLeast(0)
        }
    }

    private suspend fun assignCluster(
        embedding: FloatArray,
        clusters: MutableList<Cluster>,
        media: Media.UriMedia,
        face: DetectedFaceBox,
        bitmap: Bitmap,
        touchedPeople: MutableSet<String>
    ): String {
        var best: Cluster? = null
        var bestScore = Float.NEGATIVE_INFINITY
        clusters.forEach { cluster ->
            val score = FaceHelper.cosine(embedding, cluster.normalizedCentroid)
            if (score > bestScore) {
                best = cluster
                bestScore = score
            }
        }
        best?.takeIf { bestScore >= CLUSTER_THRESHOLD }?.let { cluster ->
            val newCount = cluster.count + 1
            cluster.centroid = FloatArray(cluster.centroid.size) { index ->
                (cluster.centroid[index] * cluster.count + embedding[index]) / newCount
            }
            cluster.normalizedCentroid = FaceHelper.l2Normalize(cluster.centroid)
            cluster.count = newCount
            touchedPeople += cluster.personId
            return cluster.personId
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
        clusters += Cluster(personId, embedding.copyOf(), embedding.copyOf(), 1)
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

    companion object {
        private const val CLUSTER_THRESHOLD = 0.45f
        private const val FACE_THUMBNAIL_DIRECTORY = "face_thumbs"
    }
}
