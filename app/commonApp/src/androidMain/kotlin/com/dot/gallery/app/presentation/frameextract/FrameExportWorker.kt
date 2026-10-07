package com.dot.gallery.app.presentation.frameextract

import android.content.Context
import android.net.Uri
import android.os.StatFs
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.WorkSignals
import com.dot.gallery.core.work.workData
import com.dot.gallery.core.work.workDataJson
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import me.tatarka.inject.annotations.Inject



class FrameExportWorker @Inject constructor(
    private val appContext: Context,
) : GalleryWorker {
    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult = withContext(Dispatchers.IO) {
        // Progress/output are keyed on the session-scoped work name so concurrent
        // sessions don't clobber each other's WorkSignals entries.
        val sessionId = input.getString(KEY_SESSION)?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }
        val signalName = sessionId?.let(::workName) ?: WORKER_NAME
        val localPath = input.getString(KEY_LOCAL_PATH)
        val localFile = localPath?.takeIf(String::isNotBlank)?.let(::File)
        val owned = input.getBoolean(KEY_OWNED_SOURCE, false)
        val sourceUri = input.getString(KEY_SOURCE_URI)?.let(Uri::parse)
            ?: return@withContext failureWithCleanup(signalName, "Prepared source is missing", owned, localFile)
        val identities = input.getStringArray(KEY_IDENTITIES)
            ?.mapNotNull(FrameIdentity::decode)
            ?.distinct()
            ?.sorted()
            ?.take(FrameSelectionReducer.MAX_SELECTION)
            .orEmpty()
        if (identities.isEmpty()) {
            return@withContext failureWithCleanup(signalName, "No frames selected", owned, localFile)
        }
        val source = decodeSource(input)
            ?: return@withContext failureWithCleanup(signalName, "Source metadata is missing", owned, localFile)
        val format = FrameExportFormat.fromPersisted(input.getString(KEY_FORMAT))
        val decoder = FrameDecoderSession(appContext, sourceUri, localFile)
        val writer = ExtractedFrameWriter(appContext.contentResolver)
        val saved = mutableListOf<String>()
        var failed = 0
        var warnings = 0
        try {
            val metadata = decoder.prepare()
            ensureOutputStorage(metadata, identities.size, format)
            identities.forEachIndexed { index, identity ->
                coroutineContext.ensureActive()
                reportProgress(signalName, progressData(PHASE_DECODING, index, identities.size))
                try {
                    val bitmap = decoder.decodeFullResolution(identity)
                    try {
                        reportProgress(signalName, progressData(PHASE_ENCODING, index, identities.size))
                        reportProgress(signalName, progressData(PHASE_WRITING_METADATA, index, identities.size))
                        val result = writer.write(bitmap, source, identity, format)
                        saved += result.uri.toString()
                        if (result.metadataWarning) warnings++
                    } finally {
                        bitmap.recycle()
                    }
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    failed++
                }
                reportProgress(signalName, progressData(PHASE_DECODING, index + 1, identities.size))
            }
            val output = workData(
                KEY_SAVED_URIS to saved.toTypedArray(),
                KEY_FAILED_COUNT to failed,
                KEY_WARNING_COUNT to warnings,
                KEY_TOTAL to identities.size,
            )
            // Publish under the session-scoped name as well — the adapter only records
            // Success data under the registered worker name, but observers key on the
            // session (matching how failure payloads above are published).
            WorkSignals.recordOutput(signalName, output)
            if (saved.isEmpty()) {
                // Failure can't carry data — publish it so WorkSignals.observe readers see it.
                WorkerResult.Failure("frame export produced no output")
            } else WorkerResult.Success(data = output)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val output = workData(
                KEY_ERROR to (error.message ?: "Frame export failed"),
                KEY_SAVED_URIS to saved.toTypedArray(),
                KEY_FAILED_COUNT to (failed + identities.size - saved.size - failed).coerceAtLeast(failed),
                KEY_WARNING_COUNT to warnings,
                KEY_TOTAL to identities.size,
            )
            WorkSignals.recordOutput(signalName, output)
            WorkerResult.Failure(error.message ?: "Frame export failed")
        } finally {
            decoder.close()
            if (owned) localFile?.let {
                FrameSourceCleanup.release(it)
                it.delete()
            }
        }
    }

    private fun failureWithCleanup(signalName: String, message: String, owned: Boolean, localFile: File?): WorkerResult {
        if (owned) localFile?.let {
            FrameSourceCleanup.release(it)
            it.delete()
        }
        WorkSignals.recordOutput(signalName, workData(KEY_ERROR to message))
        return WorkerResult.Failure(message)
    }

    private fun reportProgress(signalName: String, data: JsonObject) {
        WorkSignals.reportProgress(signalName, data)
    }

    private fun ensureOutputStorage(
        metadata: FrameVideoMetadata,
        count: Int,
        format: FrameExportFormat,
    ) {
        val bytesPerFrame = metadata.width.toLong() * metadata.height.toLong() *
            if (format == FrameExportFormat.PNG) 4L else 1L
        val required = bytesPerFrame * count + MIN_FREE_AFTER_EXPORT
        val storagePath = appContext.getExternalFilesDir(null)?.absolutePath ?: appContext.filesDir.absolutePath
        if (StatFs(storagePath).availableBytes < required) {
            throw FrameSourceException("Not enough free storage")
        }
    }

    companion object {
        const val KEY_PHASE = "phase"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_SAVED_URIS = "saved_uris"
        const val KEY_FAILED_COUNT = "failed_count"
        const val KEY_WARNING_COUNT = "warning_count"
        const val KEY_ERROR = "error"

        const val PHASE_DECODING = "DECODING"
        const val PHASE_ENCODING = "ENCODING"
        const val PHASE_WRITING_METADATA = "WRITING_METADATA"

        const val WORKER_NAME = "FrameExportWorker"
        private const val KEY_SESSION = "session"
        private const val KEY_SOURCE_URI = "source_uri"
        private const val KEY_LOCAL_PATH = "local_path"
        private const val KEY_OWNED_SOURCE = "owned_source"
        private const val KEY_IDENTITIES = "identities"
        private const val KEY_FORMAT = "format"
        private const val KEY_MEDIA_ID = "media_id"
        private const val KEY_LABEL = "label"
        private const val KEY_MIME = "mime"
        private const val KEY_SOURCE_KIND = "source_kind"
        private const val KEY_RELATIVE_PATH = "relative_path"
        private const val KEY_CAPTURE_TIMESTAMP = "capture_timestamp"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_SOURCE_SIZE = "source_size"
        private const val NO_COORDINATE = Double.NaN
        private const val MIN_FREE_AFTER_EXPORT = 128L * 1024L * 1024L

        suspend fun enqueue(
            scheduler: BackgroundTaskScheduler,
            sessionId: UUID,
            prepared: PreparedFrameSource,
            identities: List<FrameIdentity>,
            format: FrameExportFormat,
        ): String {
            val source = prepared.source
            val data = workDataJson(
                KEY_SESSION to sessionId.toString(),
                KEY_SOURCE_URI to prepared.sourceUri.toString(),
                KEY_LOCAL_PATH to prepared.localFile?.absolutePath.orEmpty(),
                KEY_OWNED_SOURCE to (prepared.ownership == FrameSourceOwnership.SESSION),
                KEY_IDENTITIES to identities.map(FrameIdentity::encode).toTypedArray(),
                KEY_FORMAT to format.persistedValue,
                KEY_MEDIA_ID to source.mediaId,
                KEY_LABEL to source.label,
                KEY_MIME to source.mimeType,
                KEY_SOURCE_KIND to source.sourceKind.name,
                KEY_RELATIVE_PATH to source.relativePath,
                KEY_CAPTURE_TIMESTAMP to source.captureTimestampMs,
                KEY_LATITUDE to (source.latitude ?: NO_COORDINATE),
                KEY_LONGITUDE to (source.longitude ?: NO_COORDINATE),
                KEY_SOURCE_SIZE to source.sourceSize,
            )
            val id = workName(sessionId)
            scheduler.enqueue(
                id = id,
                trigger = TaskTrigger.OneTime(),
                workerClassName = WORKER_NAME,
                inputJson = data,
                policy = ExistingPolicy.REPLACE,
                tags = setOf(id),
            )
            return id
        }

        fun workName(sessionId: UUID): String = "frame-export-$sessionId"

        private fun decodeSource(data: WorkInput): FrameSourceSpec? {
            val mediaId = data.getLong(KEY_MEDIA_ID, Long.MIN_VALUE)
            val label = data.getString(KEY_LABEL) ?: return null
            val mime = data.getString(KEY_MIME) ?: return null
            val sourceKind = data.getString(KEY_SOURCE_KIND)
                ?.let { runCatching { FrameSourceKind.valueOf(it) }.getOrNull() }
                ?: return null
            val latitude = data.getDouble(KEY_LATITUDE, NO_COORDINATE).takeUnless(Double::isNaN)
            val longitude = data.getDouble(KEY_LONGITUDE, NO_COORDINATE).takeUnless(Double::isNaN)
            return FrameSourceSpec(
                mediaId = mediaId,
                label = label,
                uri = data.getString(KEY_SOURCE_URI).orEmpty(),
                mimeType = mime,
                sourceKind = sourceKind,
                relativePath = data.getString(KEY_RELATIVE_PATH).orEmpty(),
                captureTimestampMs = data.getLong(KEY_CAPTURE_TIMESTAMP, 0L),
                latitude = latitude,
                longitude = longitude,
                sourceSize = data.getLong(KEY_SOURCE_SIZE, 0L),
            )
        }

        private fun progressData(phase: String, done: Int, total: Int): JsonObject =
            workData(KEY_PHASE to phase, KEY_DONE to done, KEY_TOTAL to total)
    }
}

data class FrameExportAggregate(
    val saved: List<String>,
    val failed: Int,
    val cancelled: Boolean,
    val warnings: Int,
) {
    val isPartial: Boolean get() = saved.isNotEmpty() && failed > 0
}

object FrameExportResultAggregator {
    fun aggregate(
        savedValues: Collection<String>,
        failed: Int,
        cancelled: Boolean,
        warnings: Int,
    ): FrameExportAggregate = FrameExportAggregate(
        saved = savedValues.filter(String::isNotBlank),
        failed = failed.coerceAtLeast(0),
        cancelled = cancelled,
        warnings = warnings.coerceAtLeast(0),
    )
}

@Worker(name = FrameExportWorker.WORKER_NAME)
class FrameExportWorkerAdapter : GalleryWorkerAdapter(FrameExportWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.frameExportWorker.doWork(input, env)
}
