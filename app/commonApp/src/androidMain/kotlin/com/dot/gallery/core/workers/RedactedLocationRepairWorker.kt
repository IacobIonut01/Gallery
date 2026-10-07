package com.dot.gallery.core.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.Settings
import com.dot.gallery.core.sandbox.IsolatedMetadataParser
import com.dot.gallery.data.InternalDatabase
import com.dot.gallery.data.dao.MediaDao
import com.dot.gallery.data.dao.MetadataDao
import com.dot.gallery.domain.model.Media
import com.dot.gallery.data.entity.MediaMetadata
import com.dot.gallery.domain.model.retrieveExtraMediaMetadata
import com.dot.gallery.core.logging.withLogScope
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import com.dot.gallery.core.util.or
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.data.entity.metadataParsingPolicy
import me.tatarka.inject.annotations.Inject



internal const val REDACTED_LOCATION_REPAIR_WORK = "RedactedLocationRepair"
private const val REDACTED_REPAIR_MAX_ATTEMPTS = 3

suspend fun BackgroundTaskScheduler.enqueueRedactedLocationRepair(
    policy: ExistingPolicy = ExistingPolicy.KEEP,
) {
    enqueue(
        id = REDACTED_LOCATION_REPAIR_WORK,
        trigger = TaskTrigger.OneTime(),
        workerClassName = RedactedLocationRepairWorker.WORKER_NAME,
        constraints = Constraints(maxRetries = REDACTED_REPAIR_MAX_ATTEMPTS - 1),
        policy = policy,
        tags = setOf(REDACTED_LOCATION_REPAIR_WORK),
    )
}

/**
 * Re-parses metadata rows that were captured while MediaProvider redacted GPS EXIF
 * (missing ACCESS_MEDIA_LOCATION). Those rows carry the literal 0,0 coordinate pair,
 * which nothing else produces — so once the permission is granted this worker can
 * repair exactly the poisoned rows and store either the real fix or no coordinate.
 */
class RedactedLocationRepairWorker @Inject constructor(
    private val database: InternalDatabase,
    private val geocoder: Geocoder?,
    private val isolatedParser: IsolatedMetadataParser,
    private val appContext: Context,
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        withLogScope("worker.location-repair") {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withLogScope WorkerResult.Success()
            }
            val isolationMode = Settings.Security.getMetadataIsolationMode(appContext)
                .firstOrNull() ?: Settings.Security.METADATA_ISOLATION_SHARED
            val usePerFile = isolationMode == Settings.Security.METADATA_ISOLATION_PER_FILE
            val repaired = try {
                repairRedactedMetadataLocations(
                    mediaDao = database.getMediaDao(),
                    metadataDao = database.getMetadataDao(),
                ) { media ->
                    appContext.retrieveExtraMediaMetadata(
                        isolatedParser = isolatedParser,
                        geocoder = geocoder,
                        media = media,
                        usePerFileIsolation = usePerFile,
                        policy = metadataParsingPolicy(bulk = true)
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                printWarning("Redacted location repair failed: ${error.javaClass.simpleName}")
                return@withLogScope WorkerResult.Retry(reason = "repair pass failed")
            }
            printDebug("Re-parsed $repaired metadata rows captured under GPS redaction")
            WorkerResult.Success()
        }

    companion object {
        const val WORKER_NAME = "RedactedLocationRepairWorker"
    }
}

@Worker(name = RedactedLocationRepairWorker.WORKER_NAME)
class RedactedLocationRepairWorkerAdapter :
    GalleryWorkerAdapter(RedactedLocationRepairWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.redactedLocationRepairWorker.doWork(input, env)
}

/**
 * Re-parses every metadata row whose stored coordinates are the redaction signature
 * (literal 0,0) via [parse] and upserts the result. Rows whose media vanished are
 * skipped; a parse returning null or still-0,0 coordinates leaves the row queued for
 * a later attempt. Returns the number of rows successfully re-parsed.
 */
internal suspend fun repairRedactedMetadataLocations(
    mediaDao: MediaDao,
    metadataDao: MetadataDao,
    parse: suspend (Media) -> MediaMetadata?,
): Int {
    var repaired = 0
    for (mediaId in metadataDao.getZeroCoordinateMediaIds()) {
        currentCoroutineContext().ensureActive()
        val media = runCatching { mediaDao.getMediaById(mediaId) }.getOrNull() ?: continue
        val metadata = runCatching { parse(media) }.getOrNull() ?: continue
        metadataDao.addMetadata(metadata)
        repaired++
    }
    return repaired
}
