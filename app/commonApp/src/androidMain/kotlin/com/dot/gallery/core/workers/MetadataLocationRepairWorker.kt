package com.dot.gallery.core.workers

import android.location.Address
import android.location.Geocoder
import com.dot.gallery.data.InternalDatabase
import com.dot.gallery.data.dao.GeocodedMetadataLocation
import com.dot.gallery.data.dao.MetadataDao
import com.dot.gallery.core.logging.withLogScope
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackoffPolicy as KmpBackoffPolicy
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.dot.gallery.domain.util.formattedAddress
import com.dot.gallery.domain.util.locationGroupName
import com.dot.gallery.domain.model.bestEffortReverseGeocode
import com.dot.gallery.core.logging.printDebug
import com.dot.gallery.core.logging.printWarning
import com.dot.gallery.domain.model.locationCoordinateGroupKey
import me.tatarka.inject.annotations.Inject



internal const val METADATA_LOCATION_REPAIR_WORK = "MetadataLocationRepair"
private const val METADATA_GEOCODE_REPAIR_BATCH = 32
internal const val METADATA_REPAIR_MAX_ATTEMPTS = 3

internal fun metadataLocationRepairConstraints(): Constraints = Constraints(
    requiresNetwork = true,
    backoffPolicy = KmpBackoffPolicy.EXPONENTIAL,
    backoffDelayMs = TimeUnit.MINUTES.toMillis(1),
    maxRetries = METADATA_REPAIR_MAX_ATTEMPTS - 1,
)

suspend fun BackgroundTaskScheduler.enqueueMetadataLocationRepair(
    policy: ExistingPolicy = ExistingPolicy.KEEP,
) {
    enqueue(
        id = METADATA_LOCATION_REPAIR_WORK,
        trigger = TaskTrigger.OneTime(),
        workerClassName = MetadataLocationRepairWorker.WORKER_NAME,
        constraints = metadataLocationRepairConstraints(),
        policy = policy,
        tags = setOf(METADATA_LOCATION_REPAIR_WORK),
    )
}

internal fun metadataLocationRepairResult(needsRetry: Boolean): WorkerResult =
    when {
        !needsRetry -> WorkerResult.Success()
        else -> WorkerResult.Retry(reason = "pending locations remain")
    }

class MetadataLocationRepairWorker @Inject constructor(
    private val database: InternalDatabase,
    private val geocoder: Geocoder?,
) : GalleryWorker {
    @Suppress("DEPRECATION")
    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        withLogScope("worker.metadata-repair") {
            val activeGeocoder = geocoder ?: run {
                printWarning("Location name repair unavailable: no geocoder")
                return@withLogScope WorkerResult.Success()
            }
            val needsRetry = try {
                repairPendingMetadataLocations(database.getMetadataDao()) { latitude, longitude ->
                    withContext(Dispatchers.IO) {
                        activeGeocoder.getFromLocation(latitude, longitude, 1).orEmpty().firstOrNull()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                printWarning("Location name repair failed: ${error.javaClass.simpleName}")
                true
            }
            if (needsRetry) printWarning("Location name repair incomplete")
            metadataLocationRepairResult(needsRetry)
        }

    companion object {
        const val WORKER_NAME = "MetadataLocationRepairWorker"
    }
}

@Worker(name = MetadataLocationRepairWorker.WORKER_NAME)
class MetadataLocationRepairWorkerAdapter :
    GalleryWorkerAdapter(MetadataLocationRepairWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.metadataLocationRepairWorker.doWork(input, env)
}

internal suspend fun repairPendingMetadataLocations(
    metadataDao: MetadataDao,
    lookup: suspend (Double, Double) -> Address?,
): Boolean {
    val addressByCoordinate = object : LinkedHashMap<String, Address?>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Address?>): Boolean =
            size > 256
    }
    var afterMediaId = Long.MIN_VALUE
    var examined = 0
    var repaired = 0
    var needsRetry = false
    while (true) {
        currentCoroutineContext().ensureActive()
        val pending = metadataDao.getPendingMetadataLocations(afterMediaId, METADATA_GEOCODE_REPAIR_BATCH)
        if (pending.isEmpty()) break
        afterMediaId = pending.last().mediaId
        examined += pending.size
        val updates = ArrayList<GeocodedMetadataLocation>(pending.size)
        for (item in pending) {
            currentCoroutineContext().ensureActive()
            if (item.gpsLatitude == 0.0 && item.gpsLongitude == 0.0) continue
            val coordinateKey = locationCoordinateGroupKey(item.gpsLatitude, item.gpsLongitude) ?: continue
            val address = if (addressByCoordinate.containsKey(coordinateKey)) {
                addressByCoordinate[coordinateKey]
            } else {
                bestEffortReverseGeocode(true, item.gpsLatitude, item.gpsLongitude, lookup)
                    .also { addressByCoordinate[coordinateKey] = it }
            }
            if (address == null) {
                needsRetry = true
                continue
            }
            currentCoroutineContext().ensureActive()
            val locationName = address.formattedAddress.takeIf(String::isNotBlank)
            val country = address.countryName?.takeIf(String::isNotBlank)
            val city = address.locationGroupName
            if (country == null && city == null) {
                needsRetry = true
                continue
            }
            updates += GeocodedMetadataLocation(
                mediaId = item.mediaId,
                latitude = item.gpsLatitude,
                longitude = item.gpsLongitude,
                locationName = locationName,
                country = country,
                city = city,
            )
        }
        currentCoroutineContext().ensureActive()
        if (updates.isNotEmpty()) repaired += metadataDao.updateGeocodedLocations(updates)
    }
    printDebug("Reverse geocoded $repaired of $examined pending metadata locations")
    return needsRetry
}
