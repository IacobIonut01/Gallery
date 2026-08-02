/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dot.gallery.R
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudDeleteLocalPrefDao
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.cloud.data.dao.CloudServerConfigDao
import com.dot.gallery.cloud.data.dao.CloudUploadPrefDao
import com.dot.gallery.cloud.data.entity.CloudServerConfigEntity
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.printDebug
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.dot.gallery.feature_node.domain.util.getUri
import androidx.work.workDataOf
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

@HiltWorker
class CloudUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val registry: ProviderRegistry,
    private val configDao: CloudServerConfigDao,
    private val uploadPrefDao: CloudUploadPrefDao,
    private val deleteLocalPrefDao: CloudDeleteLocalPrefDao,
    private val cloudMediaDao: CloudMediaDao,
    private val repository: MediaRepository
) : CoroutineWorker(context, workerParams) {

    /** A single asset queued for upload to a specific cloud account. */
    private data class UploadTask(
        val media: Media,
        val provider: SyncCapableProvider,
        val accountLabel: String,
        val configId: Long,
        val albumLabel: String,
        val checksum: String?,
        /**
         * Remote folder to upload into for path-based stores (WebDAV/ownCloud/Nextcloud/SMB/NFS),
         * so each backed-up local album lands in its own folder instead of a single flat "Photos"
         * dir. Content-addressable stores (Immich) ignore this. Null falls back to the provider's
         * default upload folder.
         */
        val targetPath: String?
    )

    /** A successfully-uploaded asset, tracked so its album can be synced to the server. */
    private data class UploadedAsset(
        val configId: Long,
        val albumLabel: String,
        val remoteId: String
    )

    override suspend fun doWork(): Result {
        printDebug("CloudUploadWorker: Starting upload check...")
        try {
            // Manual ("Upload now" / "Start backup") runs are user-initiated and must work even
            // when periodic auto-sync (syncEnabled) is off. syncEnabled only governs the background
            // scheduler, so only require it for the periodic worker, not for a manual trigger.
            val isManual = inputData.getBoolean(KEY_MANUAL, false)
            val targetConfigId = inputData.getLong(KEY_CONFIG_ID, -1L)
            val configs = configDao.getAll().first()
            val activeConfigs = configs.filter {
                it.isActive && (isManual || it.syncEnabled) &&
                    (targetConfigId <= 0L || it.id == targetConfigId)
            }
            if (activeConfigs.isEmpty()) {
                printDebug("CloudUploadWorker: No active server config (manual=$isManual, target=$targetConfigId)")
                return Result.success()
            }

            // Current network/charging state used to honour the per-account Backup Options:
            // "Use cellular data for photos/videos" and "Require charging".
            val isMetered = runCatching {
                (applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as? ConnectivityManager)?.isActiveNetworkMetered ?: false
            }.getOrDefault(false)
            val isCharging = (applicationContext.getSystemService(Context.BATTERY_SERVICE)
                    as? BatteryManager)?.isCharging ?: false

            // Build the upload queue per account: each account uploads ONLY the
            // local albums that have been enabled for that account.
            val tasks = mutableListOf<UploadTask>()
            val hashCache = mutableMapOf<String, String?>()

            for (config in activeConfigs) {
                // "Require charging" is a background-only constraint: a user-initiated "Upload now"
                // (isManual) always proceeds, but scheduled/periodic runs are skipped for accounts
                // that require charging while the device is not charging.
                if (!isManual && config.requireCharging && !isCharging) {
                    printDebug("CloudUploadWorker: Skipping account #${config.id} — requires charging")
                    continue
                }

                val syncProvider = registry.getByConfigId(config.id) as? SyncCapableProvider
                if (syncProvider == null) {
                    printDebug("CloudUploadWorker: Provider ${config.providerType} does not support sync, skipping")
                    continue
                }

                val enabledPrefs = uploadPrefDao.getEnabledByConfigList(config.id)
                if (enabledPrefs.isEmpty()) {
                    printDebug("CloudUploadWorker: No albums enabled for account #${config.id} (${config.providerType})")
                    continue
                }

                val accountLabel = config.displayName.ifBlank { config.providerType.displayName }
                val cachedLocalRevisions = cloudMediaDao.getByServerConfig(config.id).first()
                    .mapNotNull { entity ->
                        entity.localCopyPath.takeIf { it.isNotBlank() }?.let {
                            "$it|${entity.size}|${entity.timestamp / 1000L}"
                        }
                    }
                    .toSet()

                for (pref in enabledPrefs) {
                    // Path-based stores (WebDAV/ownCloud/Nextcloud/SMB/NFS) mirror each
                    // backed-up local album into its own remote folder named after the
                    // album, instead of flattening everything into a single "Photos" dir.
                    // Content-addressable stores (Immich) ignore this. Blank labels fall
                    // back to the provider's default upload folder (null).
                    val albumTarget = pref.albumLabel.trim().ifBlank { null }
                    val allAlbumMedia = repository.getMediaByAlbumId(pref.albumId, skipBatching = true)
                        .first().data ?: continue
                    if (allAlbumMedia.isEmpty()) continue

                    // On a metered (cellular) network, drop photos/videos the account opted out of.
                    // On unmetered networks (Wi-Fi/Ethernet) both toggles are irrelevant.
                    val albumMedia = if (!isMetered) allAlbumMedia else allAlbumMedia.filter { m ->
                        if (m.mimeType.startsWith("video/")) config.cellularVideos else config.cellularPhotos
                    }
                    if (albumMedia.isEmpty()) {
                        printDebug("CloudUploadWorker: [$accountLabel] album ${pref.albumLabel} skipped on metered network (cellular disabled)")
                        continue
                    }

                    val candidates = albumMedia.filter { media ->
                        "${media.getUri()}|${media.size}|${media.timestamp}" !in cachedLocalRevisions
                    }
                    val notPresent = candidates.filter { media ->
                        !runCatching { syncProvider.remoteExists(media, albumTarget) }.getOrDefault(false)
                    }
                    if (notPresent.isEmpty()) continue

                    if (!syncProvider.requiresUploadChecksum) {
                        notPresent.forEach { media ->
                            tasks.add(
                                UploadTask(
                                    media = media,
                                    provider = syncProvider,
                                    accountLabel = accountLabel,
                                    configId = config.id,
                                    albumLabel = pref.albumLabel,
                                    checksum = null,
                                    targetPath = albumTarget
                                )
                            )
                        }
                        continue
                    }

                    notPresent.chunked(BULK_CHECK_SIZE).forEach chunkLoop@ { chunk ->
                        val mediaWithHashes = chunk.mapNotNull { media ->
                            val hash = cachedSha1(media, hashCache) ?: return@mapNotNull null
                            media to hash
                        }
                        if (mediaWithHashes.isEmpty()) return@chunkLoop
                        val alreadyUploaded = syncProvider.bulkUploadCheck(mediaWithHashes.map { it.second })
                            .getOrElse { error ->
                                printDebug("CloudUploadWorker: Bulk check failed for $accountLabel: ${error.message}")
                                return@chunkLoop
                            }
                        mediaWithHashes.forEachIndexed { idx, (media, hash) ->
                            if (alreadyUploaded[idx.toString()] != true) {
                                tasks.add(
                                    UploadTask(
                                        media = media,
                                        provider = syncProvider,
                                        accountLabel = accountLabel,
                                        configId = config.id,
                                        albumLabel = pref.albumLabel,
                                        checksum = hash,
                                        targetPath = albumTarget
                                    )
                                )
                            }
                        }
                    }
                }
            }

            val totalItems = tasks.size
            var completedItems = 0
            var failedItems = 0
            val completedFiles = mutableListOf<String>()
            val failedFiles = mutableListOf<String>()
            val uploadedAssets = mutableListOf<UploadedAsset>()

            // Notification behaviour is a per-account preference; honour it if ANY active
            // account opted in (the workers process every active account in one run).
            val showTotalProgress = activeConfigs.any { it.showBackupTotalProgress }
            val showDetailProgress = activeConfigs.any { it.showBackupDetailProgress }
            val notifyFailures = activeConfigs.any { it.notifyBackupFailures }

            setProgress(workDataOf(
                KEY_TOTAL_ITEMS to totalItems,
                KEY_COMPLETED_ITEMS to 0,
                KEY_FAILED_ITEMS to 0,
                KEY_CURRENT_FILE to ""
            ))

            if (showTotalProgress && totalItems > 0) {
                runCatching { setForeground(progressForegroundInfo(0, totalItems, null, showDetailProgress)) }
            }

            val progressMutex = Mutex()
            for (providerTasks in tasks.groupBy { it.provider }.values) {
                runWorkerPool(
                    items = providerTasks,
                    maxConcurrency = providerTasks.first().provider.maxConcurrentUploads
                ) { task ->
                    if (isStopped) return@runWorkerPool
                    val uploadResult = try {
                        task.checksum?.let {
                            task.provider.uploadAsset(task.media, task.targetPath, it)
                        } ?: task.provider.uploadAsset(task.media, task.targetPath)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        kotlin.Result.failure(e)
                    }
                    progressMutex.withLock {
                        uploadResult.onSuccess { entity ->
                            completedItems++
                            completedFiles.add(task.media.label)
                            if (entity.remoteId.isNotBlank()) {
                                uploadedAssets.add(
                                    UploadedAsset(task.configId, task.albumLabel, entity.remoteId)
                                )
                            }
                            printDebug("CloudUploadWorker: [${task.accountLabel}] uploaded ${task.media.label}")
                        }.onFailure { e ->
                            failedItems++
                            failedFiles.add(task.media.label)
                            printDebug("CloudUploadWorker: [${task.accountLabel}] upload failed for ${task.media.label}: ${e.message}")
                        }
                        setProgress(workDataOf(
                            KEY_TOTAL_ITEMS to totalItems,
                            KEY_COMPLETED_ITEMS to completedItems,
                            KEY_FAILED_ITEMS to failedItems,
                            KEY_CURRENT_FILE to task.media.label,
                            KEY_CURRENT_ACCOUNT to task.accountLabel,
                            KEY_COMPLETED_FILES to completedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray(),
                            KEY_FAILED_FILES to failedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray()
                        ))
                        if (showTotalProgress) {
                            runCatching {
                                setForeground(
                                    progressForegroundInfo(
                                        completedItems + failedItems,
                                        totalItems,
                                        task.media.label,
                                        showDetailProgress
                                    )
                                )
                            }
                        }
                    }
                }
                if (isStopped) return Result.retry()
            }

            setProgress(workDataOf(
                KEY_TOTAL_ITEMS to totalItems,
                KEY_COMPLETED_ITEMS to completedItems,
                KEY_FAILED_ITEMS to failedItems,
                KEY_CURRENT_FILE to "",
                KEY_CURRENT_ACCOUNT to "",
                KEY_COMPLETED_FILES to completedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray(),
                KEY_FAILED_FILES to failedFiles.takeLast(MAX_TRACKED_FILES).toTypedArray()
            ))

            if (notifyFailures && failedItems > 0) {
                postFailureNotification(failedItems, failedFiles)
            }

            // "Sync albums to server": for accounts that opted in, create/match a server album
            // per local album and add the just-uploaded assets to it.
            runAlbumSyncPass(activeConfigs, uploadedAssets)

            // Gated local deletion: delete-local is a GLOBAL per-album setting, and an asset is
            // removed ONLY once it's confirmed present on EVERY cloud that album backs up to.
            // This runs after all uploads so partial fan-out never causes data loss.
            runDeleteLocalPass(activeConfigs)

            printDebug("CloudUploadWorker: Upload check complete")
            return Result.success()
        } catch (e: Exception) {
            printDebug("CloudUploadWorker: Failed: ${e.message}")
            return Result.retry()
        }
    }

    /**
     * Removes local copies for albums opted into delete-local, but ONLY for assets confirmed
     * present on EVERY cloud that album backs up to. If any destination provider is unavailable or
     * its coverage check fails, the album is skipped entirely so a partially-backed-up asset is
     * never deleted.
     */
    private suspend fun runDeleteLocalPass(activeConfigs: List<CloudServerConfigEntity>) {
        try {
            val deleteAlbums = deleteLocalPrefDao.getEnabledAlbumIds().toSet()
            if (deleteAlbums.isEmpty()) return
            val enabledByConfig = activeConfigs.associateWith { uploadPrefDao.getEnabledByConfigList(it.id) }
            for (albumId in deleteAlbums) {
                val destConfigs = activeConfigs.filter { cfg ->
                    enabledByConfig[cfg]?.any { it.albumId == albumId } == true
                }
                if (destConfigs.isEmpty()) continue
                val destProviders = destConfigs.mapNotNull { registry.getByConfigId(it.id) as? SyncCapableProvider }
                if (destProviders.size != destConfigs.size) {
                    printDebug("CloudUploadWorker: delete-local skipped for album $albumId — a destination is unavailable")
                    continue
                }
                val albumMedia = repository.getMediaByAlbumId(albumId, skipBatching = true).first().data ?: continue
                val mediaWithHashes = albumMedia.mapNotNull { m -> computeSha1(m)?.let { m to it } }
                if (mediaWithHashes.isEmpty()) continue
                val hashes = mediaWithHashes.map { it.second }
                val presence = destProviders.map { p ->
                    try {
                        p.bulkUploadCheck(hashes).getOrDefault(emptyMap())
                    } catch (e: Exception) {
                        printDebug("CloudUploadWorker: delete-local coverage check failed: ${e.message}")
                        null
                    }
                }
                if (presence.any { it == null }) continue
                mediaWithHashes.forEachIndexed { idx, (media, _) ->
                    val onAllClouds = presence.all { it!![idx.toString()] == true }
                    if (onAllClouds) {
                        try {
                            applicationContext.contentResolver.delete(media.getUri(), null, null)
                            printDebug("CloudUploadWorker: Deleted local ${media.label} (backed up to ${destProviders.size} clouds)")
                        } catch (e: Exception) {
                            printDebug("CloudUploadWorker: delete-local failed for ${media.label}: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            printDebug("CloudUploadWorker: delete-local pass failed: ${e.message}")
        }
    }

    /**
     * Honours the per-account "Sync albums to server" option. For each account that opted in,
     * groups the assets uploaded during this run by their local album name, ensures a remote
     * album with a matching name exists (reusing one if present, otherwise creating it), and
     * adds the uploaded assets to it. Best-effort: failures are logged and never fail the run.
     */
    private suspend fun runAlbumSyncPass(
        activeConfigs: List<CloudServerConfigEntity>,
        uploaded: List<UploadedAsset>
    ) {
        if (uploaded.isEmpty()) return
        try {
            val byConfig = uploaded.groupBy { it.configId }
            for (config in activeConfigs) {
                if (!config.syncAlbums) continue
                val records = byConfig[config.id]?.takeIf { it.isNotEmpty() } ?: continue
                val provider = registry.getByConfigId(config.id) as? RemoteMediaProvider ?: continue

                val remoteAlbums = runCatching { provider.getRemoteAlbums().first().data }
                    .getOrNull().orEmpty()

                for ((albumLabel, items) in records.groupBy { it.albumLabel }) {
                    if (albumLabel.isBlank()) continue
                    val assetIds = items.map { it.remoteId }.filter { it.isNotBlank() }.distinct()
                    if (assetIds.isEmpty()) continue

                    val remoteAlbumId = remoteAlbums
                        .firstOrNull { it.name.equals(albumLabel, ignoreCase = true) }?.remoteId
                        ?: runCatching { provider.createAlbum(albumLabel).getOrNull()?.remoteId }
                            .getOrNull()
                    if (remoteAlbumId.isNullOrBlank()) {
                        printDebug("CloudUploadWorker: album-sync could not resolve album '$albumLabel' for account #${config.id}")
                        continue
                    }

                    runCatching { provider.addToAlbum(remoteAlbumId, assetIds) }
                        .onSuccess {
                            printDebug("CloudUploadWorker: album-sync added ${assetIds.size} assets to '$albumLabel' (#${config.id})")
                        }
                        .onFailure { e ->
                            printDebug("CloudUploadWorker: album-sync addToAlbum failed for '$albumLabel': ${e.message}")
                        }
                }
            }
        } catch (e: Exception) {
            printDebug("CloudUploadWorker: album-sync pass failed: ${e.message}")
        }
    }

    // === Backup notifications ===

    private fun ensureChannel(id: String, nameRes: Int, importance: Int): String {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(id) == null) {
            nm.createNotificationChannel(
                NotificationChannel(id, applicationContext.getString(nameRes), importance)
            )
        }
        return id
    }

    private fun progressForegroundInfo(
        done: Int,
        total: Int,
        currentFile: String?,
        showDetail: Boolean
    ): ForegroundInfo {
        val channelId = ensureChannel(
            CHANNEL_PROGRESS, R.string.cloud_backup_channel_progress, NotificationManager.IMPORTANCE_LOW
        )
        val text = if (showDetail && !currentFile.isNullOrBlank()) {
            currentFile
        } else {
            applicationContext.getString(R.string.cloud_backup_progress_text, done, total)
        }
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setContentTitle(applicationContext.getString(R.string.cloud_backup_notification_title))
            .setContentText(text)
            .setProgress(total, done, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID_PROGRESS, notification)
        }
    }

    private fun postFailureNotification(failed: Int, failedFiles: List<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        val channelId = ensureChannel(
            CHANNEL_STATUS, R.string.cloud_backup_channel_status, NotificationManager.IMPORTANCE_DEFAULT
        )
        val title = applicationContext.getString(R.string.cloud_backup_failed_title)
        val text = applicationContext.getString(R.string.cloud_backup_failed_text, failed)
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
        if (failedFiles.isNotEmpty()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    text + "\n" + failedFiles.takeLast(10).joinToString("\n")
                )
            )
        }
        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID_STATUS, builder.build())
        }
    }

    private fun cachedSha1(media: Media, cache: MutableMap<String, String?>): String? {
        val key = "${media.getUri()}|${media.size}|${media.timestamp}"
        if (cache.containsKey(key)) return cache[key]
        return computeSha1(media).also { cache[key] = it }
    }

    private fun computeSha1(media: Media): String? {
        return try {
            val uri = media.getUri()
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("SHA-1")
                val buffer = ByteArray(HASH_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val WORK_NAME = "cloud_upload"
        const val WORK_NAME_ONCE = "cloud_upload_now"

        const val KEY_CURRENT_FILE = "current_file"
        const val KEY_CURRENT_ACCOUNT = "current_account"
        const val KEY_TOTAL_ITEMS = "total_items"
        const val KEY_COMPLETED_ITEMS = "completed_items"
        const val KEY_FAILED_ITEMS = "failed_items"
        const val KEY_COMPLETED_FILES = "completed_files"
        const val KEY_FAILED_FILES = "failed_files"
        const val KEY_MANUAL = "manual"
        const val KEY_CONFIG_ID = "config_id"
        /** Common tag for per-account manual backups; the per-account tag is "$TAG_ACCOUNT_BACKUP:<configId>". */
        const val TAG_ACCOUNT_BACKUP = "cloud_upload_account"
        /**
         * Tag applied to EVERY backup run (periodic, "back up all", and per-account manual),
         * so UI progress observers and the media distributor can track/react to any upload
         * regardless of its unique work name.
         */
        const val TAG_BACKUP = "cloud_upload_backup"
        private const val MAX_TRACKED_FILES = 50
        private const val HASH_BUFFER_SIZE = 64 * 1024
        private const val BULK_CHECK_SIZE = 500

        // Backup notification channels + ids.
        private const val CHANNEL_PROGRESS = "cloud_backup_progress"
        private const val CHANNEL_STATUS = "cloud_backup_status"
        private const val NOTIFICATION_ID_PROGRESS = 91001
        private const val NOTIFICATION_ID_STATUS = 91002

        fun schedule(
            workManager: WorkManager,
            intervalMinutes: Long = 30,
            wifiOnly: Boolean = true
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()

            val request = PeriodicWorkRequestBuilder<CloudUploadWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(TAG_BACKUP)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Runs a manual backup immediately. When [configId] is > 0 the run is
         * scoped to a single account and enqueued under its own unique work name
         * (and tagged so the UI can track that account's progress independently);
         * otherwise every active account is backed up.
         */
        fun triggerNow(workManager: WorkManager, configId: Long = -1L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val builder = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_MANUAL to true, KEY_CONFIG_ID to configId))
                .addTag(TAG_BACKUP)
            val uniqueName = if (configId > 0L) "${WORK_NAME_ONCE}_$configId" else WORK_NAME_ONCE
            if (configId > 0L) {
                builder.addTag(TAG_ACCOUNT_BACKUP)
                builder.addTag("$TAG_ACCOUNT_BACKUP:$configId")
            }
            workManager.enqueueUniqueWork(
                uniqueName,
                ExistingWorkPolicy.REPLACE,
                builder.build()
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }
    }
}

internal suspend fun <T> runWorkerPool(
    items: List<T>,
    maxConcurrency: Int,
    processItem: suspend (T) -> Unit
) = coroutineScope {
    if (items.isEmpty()) return@coroutineScope
    val nextIndex = AtomicInteger(0)
    List(min(maxConcurrency.coerceAtLeast(1), items.size)) {
        launch {
            while (true) {
                coroutineContext.ensureActive()
                val index = nextIndex.getAndIncrement()
                if (index >= items.size) break
                processItem(items[index])
            }
        }
    }.joinAll()
}
