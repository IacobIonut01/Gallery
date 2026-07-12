/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.R
import com.dot.gallery.core.ml.DownloadInfo
import com.dot.gallery.core.ml.ModelGroup
import com.dot.gallery.core.ml.ModelManager
import com.dot.gallery.feature_node.presentation.util.printInfo
import com.dot.gallery.feature_node.presentation.util.printWarning
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    private val modelManager: ModelManager,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Which feature's models this worker instance is responsible for.
        val group = inputData.getString(KEY_GROUP)
            ?.let { runCatching { ModelGroup.valueOf(it) }.getOrNull() }
            ?: ModelGroup.SEARCH
        try {
            setProgress(workDataOf(KEY_PROGRESS to 0f, KEY_STATUS to "Starting download..."))
            modelManager.updateDownloadProgress(group, 0f)

            // Create notification channel and show foreground notification
            createNotificationChannel()
            setForeground(createForegroundInfo(0))

            // Check available storage
            val statFs = StatFs(appContext.filesDir.path)
            val availableBytes = statFs.availableBytes
            if (availableBytes < MINIMUM_REQUIRED_BYTES) {
                val availableMb = availableBytes / (1024 * 1024)
                val requiredMb = MINIMUM_REQUIRED_BYTES / (1024 * 1024)
                val error = "Not enough storage (need ${requiredMb} MB, ${availableMb} MB available)"
                modelManager.onDownloadFailed(group, error)
                setProgress(workDataOf(KEY_PROGRESS to 0f, KEY_STATUS to error))
                return@withContext Result.failure(workDataOf(KEY_ERROR to error))
            }

            val modelsDir = modelManager.modelsDir
            modelsDir.mkdirs()

            val filesToDownload = group.files
            val totalFiles = filesToDownload.size
            var completedFiles = 0

            // Query total download size for all files
            var totalAllBytes = 0L
            val fileSizes = mutableMapOf<String, Long>()
            filesToDownload.forEach { fileName ->
                val destFile = modelManager.getDestinationFile(fileName)
                if (destFile.exists() && destFile.length() > 0) {
                    fileSizes[fileName] = destFile.length()
                } else {
                    try {
                        val fileUrl = when (fileName) {
                            "mobile_sam_image_encoder.onnx" -> "https://huggingface.co/Acly/MobileSAM/resolve/main/mobile_sam_image_encoder.onnx"
                            "sam_mask_decoder_single.onnx" -> "https://huggingface.co/Acly/MobileSAM/resolve/main/sam_mask_decoder_single.onnx"
                            else -> "${ModelManager.BASE_DOWNLOAD_URL}$fileName"
                        }
                        val conn = URL(fileUrl).openConnection() as HttpURLConnection
                        conn.requestMethod = "HEAD"
                        conn.connectTimeout = 15_000
                        conn.connect()
                        val len = conn.contentLengthLong
                        fileSizes[fileName] = if (len > 0) len else 0L
                        conn.disconnect()
                    } catch (_: Exception) {
                        fileSizes[fileName] = 0L
                    }
                }
            }
            totalAllBytes = fileSizes.values.sum()
            var downloadedBytes = 0L

            filesToDownload.forEach { fileName ->
                if (!currentCoroutineContext().isActive || isStopped) {
                    cleanupPartialFiles(filesToDownload)
                    return@withContext Result.failure()
                }

                val destFile = modelManager.getDestinationFile(fileName)
                destFile.parentFile?.mkdirs()

                // Skip files that already exist and are non-empty
                if (destFile.exists() && destFile.length() > 0) {
                    downloadedBytes += destFile.length()
                    completedFiles++
                    val overallProgress = (completedFiles.toFloat() / totalFiles) * 100f
                    modelManager.updateDownloadProgress(group, overallProgress)
                    setProgress(workDataOf(KEY_PROGRESS to overallProgress, KEY_STATUS to "Skipping $fileName (already exists)"))
                    printInfo("ModelDownloadWorker: Skipping $fileName (already exists)")
                    return@forEach
                }

                val url = when (fileName) {
                    "mobile_sam_image_encoder.onnx" -> "https://huggingface.co/Acly/MobileSAM/resolve/main/mobile_sam_image_encoder.onnx"
                    "sam_mask_decoder_single.onnx" -> "https://huggingface.co/Acly/MobileSAM/resolve/main/sam_mask_decoder_single.onnx"
                    else -> "${ModelManager.BASE_DOWNLOAD_URL}$fileName"
                }
                val tempFile = modelManager.getTempFile(fileName)
                tempFile.parentFile?.mkdirs()

                try {
                    downloadFile(group, url, tempFile, fileName, completedFiles, totalFiles, downloadedBytes, totalAllBytes)

                    // Atomic rename
                    if (!tempFile.renameTo(destFile)) {
                        tempFile.inputStream().use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempFile.delete()
                    }

                    downloadedBytes += destFile.length()
                    completedFiles++
                    val overallProgress = (completedFiles.toFloat() / totalFiles) * 100f
                    modelManager.updateDownloadProgress(group, overallProgress)
                    setProgress(workDataOf(KEY_PROGRESS to overallProgress, KEY_STATUS to "Downloaded $fileName"))
                    printInfo("ModelDownloadWorker: Downloaded $fileName")

                } catch (e: Exception) {
                    tempFile.delete()
                    throw e
                }
            }

            // Validate all files are present
            modelManager.onDownloadComplete(group)
            setProgress(workDataOf(KEY_PROGRESS to 100f, KEY_STATUS to "Complete"))
            printInfo("ModelDownloadWorker: All models downloaded successfully")
            return@withContext Result.success()

        } catch (e: IOException) {
            // Transient network error — let WorkManager retry with backoff
            val error = "Download failed (will retry): ${e.message}"
            modelManager.onDownloadFailed(group, error)
            setProgress(workDataOf(KEY_PROGRESS to 0f, KEY_STATUS to error))
            printWarning("ModelDownloadWorker: $error")
            return@withContext Result.retry()
        } catch (e: Exception) {
            // Non-transient error — permanent failure
            val error = "Download failed: ${e.message}"
            modelManager.onDownloadFailed(group, error)
            setProgress(workDataOf(KEY_PROGRESS to 0f, KEY_STATUS to error))
            printWarning("ModelDownloadWorker: $error")
            return@withContext Result.failure(workDataOf(KEY_ERROR to error))
        }
    }

    private fun downloadFile(
        group: ModelGroup,
        url: String,
        destFile: File,
        fileName: String,
        completedFiles: Int,
        totalFiles: Int,
        previousFilesBytes: Long,
        totalAllBytes: Long
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000
        connection.requestMethod = "GET"

        try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("HTTP $responseCode for $url")
            }

            val contentLength = connection.contentLengthLong
            var bytesRead = 0L
            val startTime = System.currentTimeMillis()

            connection.inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        if (contentLength > 0) {
                            val fileProgress = bytesRead.toFloat() / contentLength.toFloat()
                            val overallProgress = ((completedFiles + fileProgress) / totalFiles) * 100f
                            modelManager.updateDownloadProgress(group, overallProgress)

                            // Calculate speed
                            val elapsed = System.currentTimeMillis() - startTime
                            val speed = if (elapsed > 0) (bytesRead * 1000L) / elapsed else 0L
                            val totalDownloaded = previousFilesBytes + bytesRead
                            modelManager.updateDownloadInfo(
                                group,
                                DownloadInfo(
                                    speed = speed,
                                    downloadedBytes = totalDownloaded,
                                    totalBytes = totalAllBytes,
                                    currentFile = fileName
                                )
                            )

                            // Update notification periodically (~every 1MB)
                            if (bytesRead % (1024 * 1024) < 65536) {
                                setProgressAsync(workDataOf(
                                    KEY_PROGRESS to overallProgress,
                                    KEY_STATUS to "Downloading $fileName... ${(fileProgress * 100).toInt()}%"
                                ))
                                try {
                                    setForegroundAsync(createForegroundInfo(overallProgress.toInt()))
                                } catch (_: Exception) { }
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanupPartialFiles(files: List<String>) {
        files.forEach { fileName ->
            modelManager.getTempFile(fileName).delete()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI Model Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Downloads for AI model files"
        }
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle(appContext.getString(R.string.ai_models_downloading))
            .setContentText("${progress}%")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    companion object {
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_GROUP = "group"
        const val WORK_NAME = "ModelDownloadWorker"
        const val TAG = "ModelDownload"

        /** Unique work name per group so each feature's download runs and is tracked independently. */
        fun workName(group: ModelGroup) = "${WORK_NAME}_${group.name}"
        const val CHANNEL_ID = "model_download"
        const val NOTIFICATION_ID = 42042

        // ~150 MB to be safe (models are ~80 MB but we need temp space)
        const val MINIMUM_REQUIRED_BYTES = 150L * 1024 * 1024
    }
}

/**
 * Extension function to start a model download for a single feature [group].
 */
fun WorkManager.downloadModels(group: ModelGroup) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresStorageNotLow(true)
        .build()

    val request = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
        .setConstraints(constraints)
        .addTag(ModelDownloadWorker.TAG)
        .setInputData(workDataOf(ModelDownloadWorker.KEY_GROUP to group.name))
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30_000L, java.util.concurrent.TimeUnit.MILLISECONDS)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            }
        }
        .build()

    enqueueUniqueWork(
        ModelDownloadWorker.workName(group),
        ExistingWorkPolicy.KEEP,
        request
    )
}

/**
 * Extension function to cancel a feature [group]'s model download.
 */
fun WorkManager.cancelModelDownload(group: ModelGroup) {
    cancelUniqueWork(ModelDownloadWorker.workName(group))
}
