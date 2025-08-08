package com.dot.gallery.feature_node.presentation.huesearch

import android.content.Context
import androidx.compose.ui.util.fastForEachIndexed
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.domain.model.MediaVersion
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.util.mediaStoreVersion
import com.dot.gallery.feature_node.presentation.util.printWarning
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@HiltWorker
class HueClassifierWorker @AssistedInject constructor(
    private val database: InternalDatabase,
    private val repository: MediaRepository,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            val blacklisted = database.getBlacklistDao().getBlacklistedAlbumsAsync()
            var media = database.getMediaDao().getMediaByType(AllowedMedia.PHOTOS.toString())
                .filterNot { item -> blacklisted.any { it.matchesMedia(item) } }
            if (media.isEmpty()) {
                printWarning("ShadeClassifierWorker media is empty, let's try and update the database")
                val mediaVersion = appContext.mediaStoreVersion
                printWarning("ShadeClassifierWorker Force-updating database to version $mediaVersion")
                database.getMediaDao().setMediaVersion(MediaVersion(mediaVersion))
                val fetchedMedia =
                    repository.getMedia().map { it.data ?: emptyList() }.firstOrNull()
                fetchedMedia?.let {
                    database.getMediaDao().updateMedia(it)
                }
            }

            val classified = database.getHueClassifierDao().getClassifiedMedia()
            media =
                media.filterNot { item -> classified.any { it.id == item.id && it.timestamp == item.timestamp } }
            if (media.isEmpty()) {
                printWarning("ShadeClassifierWorker media is empty, we can abort")
                setProgress(workDataOf("progress" to 100))
                return@withContext Result.success()
            }
            printWarning("ShadeClassifierWorker unclassified media size: ${media.size}")
            setProgress(workDataOf("progress" to 0))

            media.fastForEachIndexed { index, item ->
                try {
                    setProgress(workDataOf("progress" to (index / media.size.toFloat()) * 100f))
                    val classifiedItem = HueClassifierHelper(appContext).classify(item)
                    database.getHueClassifierDao().insertClassifiedMedia(classifiedItem)
                } catch (e: Exception) {
                    printWarning("ShadeClassifierWorker failed to classify media: ${item.id}")
                    e.printStackTrace()
                    return@fastForEachIndexed
                }
            }
            setProgress(workDataOf("progress" to 100f))
        }
        return Result.success()
    }
}

fun WorkManager.startHueClassification(indexStart: Int = 0, size: Int = 50) {
    val inputData = Data.Builder()
        .putInt("chunkIndexStart", indexStart)
        .putInt("chunkSize", size)
        .build()
    val uniqueWork = OneTimeWorkRequestBuilder<HueClassifierWorker>()
        .addTag("HueClassifier")
        .setConstraints(
            Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()
        )
        .setInputData(inputData)
        .build()

    enqueueUniqueWork(
        "HueClassifierWorker_${indexStart}_$size",
        ExistingWorkPolicy.REPLACE,
        uniqueWork
    )
}

fun WorkManager.stopHueClassification() {
    cancelAllWorkByTag("HueClassifier")
}