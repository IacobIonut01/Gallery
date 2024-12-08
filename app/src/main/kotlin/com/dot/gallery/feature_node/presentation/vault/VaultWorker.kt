package com.dot.gallery.feature_node.presentation.vault

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun WorkManager.scheduleEncryptingMedia(
    vault: Vault,
    media: List<Media.UriMedia>,
) {
    val uniqueWork = OneTimeWorkRequestBuilder<VaultWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build()
        )
        .setInputData(
            workDataOf(
                "targetMedia" to Json.encodeToString(media.map { it.id to it.uri.toString() }),
                "targetVault" to Json.encodeToString(vault)
            )
        )
        .build()

    enqueueUniqueWork("VaultWorker_${vault.uuid}", ExistingWorkPolicy.KEEP, uniqueWork)
}

@HiltWorker
class VaultWorker @AssistedInject constructor(
    private val repository: MediaRepository,
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            val targetMedia = inputData.getString("targetMedia")
            val targetVault = inputData.getString("targetVault") ?: return@withContext Result.failure()
            if (targetMedia.isNullOrEmpty()) {
                return@withContext Result.failure()
            } else {
                val decodedTargetMedia = Json.decodeFromString<List<Pair<Long, String>>>(targetMedia)
                val decodedTargetVault = Json.decodeFromString<Vault>(targetVault)
                decodedTargetMedia.forEach { (id, uriString) ->
                    // Create a fake media object as there's only id and uri required for encryption
                    val fakeMedia = Media.UriMedia(
                        id = id,
                        uri = Uri.parse(uriString),
                        label = "",
                        path = "",
                        relativePath = "",
                        albumID = -1,
                        albumLabel = "",
                        timestamp = -1,
                        expiryTimestamp = null,
                        takenTimestamp = null,
                        fullDate = "",
                        mimeType = "",
                        favorite = 0,
                        trashed = 0,
                        size = 0,
                        duration = null,
                    )
                    if (repository.addMedia(vault = decodedTargetVault, media = fakeMedia)) {
                        return@withContext Result.success()
                    } else {
                        return@withContext Result.failure()
                    }
                }
            }
        }

        return Result.success()
    }
}

