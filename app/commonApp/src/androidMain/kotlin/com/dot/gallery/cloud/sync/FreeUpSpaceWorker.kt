/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.dot.gallery.core.work.WorkComponentHolder
import com.dot.gallery.core.work.GalleryWorker
import com.dot.gallery.core.work.GalleryWorkerAdapter
import com.dot.gallery.core.work.WorkInput
import com.dot.gallery.core.work.workDataJson
import dev.brewkits.kmpworkmanager.annotations.Worker
import dev.brewkits.kmpworkmanager.background.domain.BackgroundTaskScheduler
import dev.brewkits.kmpworkmanager.background.domain.Constraints
import dev.brewkits.kmpworkmanager.background.domain.ExistingPolicy
import dev.brewkits.kmpworkmanager.background.domain.TaskTrigger
import dev.brewkits.kmpworkmanager.background.domain.WorkerEnvironment
import dev.brewkits.kmpworkmanager.background.domain.WorkerResult
import com.dot.gallery.data.dao.CloudServerConfigDao
import com.dot.gallery.data.repository.MediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import me.tatarka.inject.annotations.Inject
import com.dot.gallery.core.injection.Singleton
import com.dot.gallery.core.ui.components.settings
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.core.util.or
import com.dot.gallery.core.presentation.util.add
import com.dot.gallery.core.presentation.util.clear
import com.dot.gallery.core.presentation.util.remove
import com.dot.gallery.core.activeDataStore
import com.dot.gallery.core.logging.printError
import com.dot.gallery.R
import com.dot.gallery.generated.resources.Res
import com.dot.gallery.generated.resources.cloud_free_space
import com.dot.gallery.generated.resources.cloud_free_space_auto_removed
import com.dot.gallery.generated.resources.cloud_free_space_auto_review
import com.dot.gallery.core.util.getString


/**
 * Scheduled twin of the interactive Free Up Space flow. On each run it reads the
 * same cutoff/favorites prefs the screen writes, verifies local copies against
 * every configured destination via [FreeUpSpaceEngine], then either removes them
 * directly — when the app holds a silent-delete grant (All Files Access, the
 * media-management role, or pre-scoped-storage) — or posts a notification asking
 * the user to review the verified set manually.
 */
class FreeUpSpaceWorker @Inject constructor(
    private val appContext: Context,
    private val engine: FreeUpSpaceEngine,
    private val repository: MediaRepository
) : GalleryWorker {

    override suspend fun doWork(input: WorkInput, env: WorkerEnvironment): WorkerResult {
        val configId = input.getLong(EXTRA_CONFIG_ID, -1L)
        if (configId == -1L) return WorkerResult.Success()
        val prefs = runCatching { appContext.activeDataStore.data.first() }.getOrNull()
            ?: return WorkerResult.Retry("prefs unavailable", attemptCap = MAX_ATTEMPTS)
        val cutoffDays = prefs[cutoffDaysKey(configId)] ?: FREE_UP_SPACE_DEFAULT_CUTOFF_DAYS
        if (cutoffDays == FREE_UP_SPACE_NEVER_CUTOFF) return WorkerResult.Success()
        val keepFavorites = prefs[keepFavoritesKey(configId)] ?: true

        val scan = try {
            engine.scan(cutoffDays, keepFavorites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            printError("worker.free-space", "scheduled free-space scan failed", e)
            null
        } ?: return WorkerResult.Retry("scan failed", attemptCap = MAX_ATTEMPTS)
        if (scan.verified.isEmpty()) return WorkerResult.Success()

        if (!repository.canDeleteMediaSilently) {
            // A background run cannot show the MediaStore consent dialog — hand
            // the verified set to the interactive screen instead.
            postReviewNotification(configId, scan.verified.size)
            return WorkerResult.Success()
        }

        // Local-only recheck: the scan just verified remote presence, but a long
        // scan leaves room for local changes (new edits, favorites, deletes) —
        // drop anything that no longer matches before touching the filesystem.
        val stillDeletable = try {
            engine.currentlyDeletable(
                scan.verified, cutoffDays, keepFavorites, scan.verifiedHashes
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            printError("worker.free-space", "deletion re-check failed", e)
            null
        } ?: return WorkerResult.Retry("deletion re-check failed", attemptCap = MAX_ATTEMPTS)

        var deleted = 0
        stillDeletable.chunked(FREE_UP_SPACE_DELETE_BATCH_SIZE).forEach { batch ->
            if (repository.deleteMediaDirectly(batch)) deleted += batch.size
        }
        if (deleted > 0) {
            postRemovedNotification(configId, deleted)
        } else {
            // The silent-delete grant reported OK but every batch failed (e.g.
            // the platform denied the write anyway) — fall back to interactive
            // review so the space is not silently retained.
            postReviewNotification(configId, stillDeletable.size)
        }
        return WorkerResult.Success()
    }

    private fun ensureChannel(): String {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_STATUS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_STATUS,
                    appContext.getString(Res.string.cloud_free_space),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        return CHANNEL_STATUS
    }

    private fun launchPendingIntent(): PendingIntent? {
        val intent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName) ?: return null
        return PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postReviewNotification(configId: Long, count: Int) {
        if (!canPostNotifications()) return
        val builder = NotificationCompat.Builder(appContext, ensureChannel())
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setContentTitle(appContext.getString(Res.string.cloud_free_space))
            .setContentText(appContext.getString(Res.string.cloud_free_space_auto_review, count))
            .setAutoCancel(true)
        launchPendingIntent()?.let(builder::setContentIntent)
        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_ID_REVIEW + configId.toInt(), builder.build())
        }
    }

    private fun postRemovedNotification(configId: Long, count: Int) {
        if (!canPostNotifications()) return
        val builder = NotificationCompat.Builder(appContext, ensureChannel())
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setContentTitle(appContext.getString(Res.string.cloud_free_space))
            .setContentText(appContext.getString(Res.string.cloud_free_space_auto_removed, count))
            .setAutoCancel(true)
        launchPendingIntent()?.let(builder::setContentIntent)
        runCatching {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIFICATION_ID_STATUS + configId.toInt(), builder.build())
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_CONFIG_ID = "configId"
        const val LEGACY_WORK_NAME = "free_up_space_periodic"
        fun workName(configId: Long) = "free_up_space_periodic_$configId"
        private const val CHANNEL_STATUS = "free_up_space_status"
        private const val NOTIFICATION_ID_STATUS = 4301
        private const val NOTIFICATION_ID_REVIEW = 4302
        const val WORKER_NAME = "FreeUpSpaceWorker"
        private const val MAX_ATTEMPTS = 3
    }
}

@Worker(name = FreeUpSpaceWorker.WORKER_NAME)
class FreeUpSpaceWorkerAdapter : GalleryWorkerAdapter(FreeUpSpaceWorker.WORKER_NAME) {
    override suspend fun run(input: WorkInput, env: WorkerEnvironment): WorkerResult =
        WorkComponentHolder.component.freeUpSpaceWorker.doWork(input, env)
}

/**
 * Owns the periodic [FreeUpSpaceWorker] schedule — one unique work item per
 * cloud account, keyed by config id, so each provider's automatic cleanup runs
 * on its own interval and follows its own settings. The run needs network for
 * the remote verification pass and is kept off metered networks/charge like
 * other bulk cloud operations.
 */
@Singleton
class FreeUpSpaceAutoScheduler @Inject constructor(
    private val scheduler: BackgroundTaskScheduler,
    private val appContext: Context,
    private val configDao: CloudServerConfigDao
) {
    /** Reconcile every account's schedule — called at app startup so toggles,
     * interval changes and removed accounts all settle into the right state. */
    suspend fun syncAll() {
        val prefs = runCatching { appContext.activeDataStore.data.first() }.getOrNull()
            ?: return
        val configs = runCatching { configDao.getAll().first() }.getOrNull() ?: return
        val liveIds = configs.map { it.id }.toSet()
        val scheduledIds = prefs[SCHEDULED_CONFIG_IDS_KEY]
            .orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        // Pre-per-provider builds scheduled a single global worker under this
        // name — clear it so upgrades can't leave a stray job behind.
        scheduler.cancel(FreeUpSpaceWorker.LEGACY_WORK_NAME)
        for (config in configs) {
            val enabled = config.syncEnabled &&
                    (prefs[autoEnabledKey(config.id)] ?: false)
            enqueueOrCancel(
                config.id, enabled,
                prefs[autoIntervalDaysKey(config.id)] ?: FREE_UP_SPACE_DEFAULT_INTERVAL_DAYS
            )
        }
        // Accounts that were removed (or had their id change) leave unique work
        // behind — cancel anything scheduled for a config that no longer exists.
        val nowScheduled = liveIds.filter { id ->
            configs.firstOrNull { it.id == id }?.syncEnabled == true &&
                    (prefs[autoEnabledKey(id)] ?: false)
        }.toSet()
        (scheduledIds - nowScheduled).forEach { stale ->
            scheduler.cancel(FreeUpSpaceWorker.workName(stale))
        }
        if (scheduledIds != nowScheduled) {
            appContext.activeDataStore.edit {
                it[SCHEDULED_CONFIG_IDS_KEY] = nowScheduled.map(Long::toString).toSet()
            }
        }
    }

    /** Read this account's stored auto prefs and apply them — used by the
     * settings screen after each toggle/interval change. */
    suspend fun sync(configId: Long) {
        val prefs = runCatching { appContext.activeDataStore.data.first() }.getOrNull()
            ?: return
        sync(
            configId = configId,
            enabled = prefs[autoEnabledKey(configId)] ?: false,
            intervalDays = prefs[autoIntervalDaysKey(configId)]
                ?: FREE_UP_SPACE_DEFAULT_INTERVAL_DAYS
        )
    }

    suspend fun sync(configId: Long, enabled: Boolean, intervalDays: Int) {
        enqueueOrCancel(configId, enabled, intervalDays)
        appContext.activeDataStore.edit {
            val current = it[SCHEDULED_CONFIG_IDS_KEY].orEmpty()
                .mapNotNull(String::toLongOrNull).toMutableSet()
            if (enabled) current.add(configId) else current.remove(configId)
            it[SCHEDULED_CONFIG_IDS_KEY] = current.map(Long::toString).toSet()
        }
    }

    private suspend fun enqueueOrCancel(configId: Long, enabled: Boolean, intervalDays: Int) {
        val name = FreeUpSpaceWorker.workName(configId)
        if (!enabled) {
            scheduler.cancel(name)
            return
        }
        scheduler.enqueue(
            id = name,
            trigger = TaskTrigger.Periodic(
                intervalMs = TimeUnit.DAYS.toMillis(intervalDays.toLong().coerceAtLeast(1))
            ),
            workerClassName = FreeUpSpaceWorker.WORKER_NAME,
            inputJson = workDataJson(FreeUpSpaceWorker.EXTRA_CONFIG_ID to configId),
            constraints = Constraints(
                requiresNetwork = true,
                requiresUnmeteredNetwork = true,
                requiresCharging = true,
            ),
            policy = ExistingPolicy.UPDATE,
        )
    }
}
