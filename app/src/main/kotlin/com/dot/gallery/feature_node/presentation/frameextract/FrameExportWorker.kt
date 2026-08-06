package com.dot.gallery.feature_node.presentation.frameextract

import android.content.Context
import android.net.Uri
import android.os.StatFs
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext

@HiltWorker
class FrameExportWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localPath = inputData.getString(KEY_LOCAL_PATH)
        val localFile = localPath?.takeIf(String::isNotBlank)?.let(::File)
        val owned = inputData.getBoolean(KEY_OWNED_SOURCE, false)
        val sourceUri = inputData.getString(KEY_SOURCE_URI)?.let(Uri::parse)
            ?: return@withContext failureWithCleanup("Prepared source is missing", owned, localFile)
        val identities = inputData.getStringArray(KEY_IDENTITIES)
            ?.mapNotNull(FrameIdentity::decode)
            ?.distinct()
            ?.sorted()
            ?.take(FrameSelectionReducer.MAX_SELECTION)
            .orEmpty()
        if (identities.isEmpty()) {
            return@withContext failureWithCleanup("No frames selected", owned, localFile)
        }
        val source = decodeSource(inputData)
            ?: return@withContext failureWithCleanup("Source metadata is missing", owned, localFile)
        val format = FrameExportFormat.fromPersisted(inputData.getString(KEY_FORMAT))
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
                setProgress(progressData(PHASE_DECODING, index, identities.size))
                try {
                    val bitmap = decoder.decodeFullResolution(identity)
                    try {
                        setProgress(progressData(PHASE_ENCODING, index, identities.size))
                        setProgress(progressData(PHASE_WRITING_METADATA, index, identities.size))
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
                setProgress(progressData(PHASE_DECODING, index + 1, identities.size))
            }
            val output = workDataOf(
                KEY_SAVED_URIS to saved.toTypedArray(),
                KEY_FAILED_COUNT to failed,
                KEY_WARNING_COUNT to warnings,
                KEY_TOTAL to identities.size,
            )
            if (saved.isEmpty()) Result.failure(output) else Result.success(output)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Result.failure(
                workDataOf(
                    KEY_ERROR to (error.message ?: "Frame export failed"),
                    KEY_SAVED_URIS to saved.toTypedArray(),
                    KEY_FAILED_COUNT to (failed + identities.size - saved.size - failed).coerceAtLeast(failed),
                    KEY_WARNING_COUNT to warnings,
                    KEY_TOTAL to identities.size,
                )
            )
        } finally {
            decoder.close()
            if (owned) localFile?.let {
                FrameSourceCleanup.release(it)
                it.delete()
            }
        }
    }

    private fun failureWithCleanup(message: String, owned: Boolean, localFile: File?): Result {
        if (owned) localFile?.let {
            FrameSourceCleanup.release(it)
            it.delete()
        }
        return Result.failure(errorData(message))
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

        fun enqueue(
            workManager: WorkManager,
            sessionId: UUID,
            prepared: PreparedFrameSource,
            identities: List<FrameIdentity>,
            format: FrameExportFormat,
        ): UUID {
            val source = prepared.source
            val data = workDataOf(
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
            val request = OneTimeWorkRequestBuilder<FrameExportWorker>()
                .setInputData(data)
                .addTag(workName(sessionId))
                .build()
            workManager.enqueueUniqueWork(workName(sessionId), ExistingWorkPolicy.REPLACE, request)
            return request.id
        }

        fun workName(sessionId: UUID): String = "frame-export-$sessionId"

        private fun decodeSource(data: Data): FrameSourceSpec? {
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

        private fun progressData(phase: String, done: Int, total: Int): Data =
            workDataOf(KEY_PHASE to phase, KEY_DONE to done, KEY_TOTAL to total)

        private fun errorData(message: String): Data = workDataOf(KEY_ERROR to message)
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
