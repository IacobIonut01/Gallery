package com.dot.gallery.core

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.compositionLocalOf
import androidx.work.WorkManager
import com.dot.gallery.cloud.core.ProviderRegistry
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.capabilities.RemoteMediaProvider
import com.dot.gallery.cloud.core.capabilities.SyncCapableProvider
import com.dot.gallery.cloud.data.dao.CloudMediaDao
import com.dot.gallery.core.Settings.Misc.getTrashEnabled
import com.dot.gallery.core.workers.VaultOperationWorker
import com.dot.gallery.core.workers.enqueueVaultOperation
import com.dot.gallery.core.util.SdkCompat
import com.dot.gallery.core.workers.rotateImage
import com.dot.gallery.feature_node.domain.util.getUri
import com.dot.gallery.feature_node.domain.util.isCloud
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.presentation.util.sdcardRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject

val LocalMediaHandler = compositionLocalOf<MediaHandler> {
    error("No MediaHandler provided!!! This is likely due to a missing Hilt injection in the Composable hierarchy.")
}

class MediaHandlerImpl @Inject constructor(
    private val repository: MediaRepository,
    private val context: Context,
    private val workManager: WorkManager,
    private val providerRegistry: ProviderRegistry,
    private val cloudMediaDao: CloudMediaDao
) : MediaHandler {

    private fun <T : Media> extractCloudInfo(media: T): Pair<String, String>? {
        if (!media.isCloud) return null
        val uri = media.getUri()
        val providerName = uri.authority ?: return null
        val remoteId = uri.pathSegments.firstOrNull() ?: return null
        return providerName to remoteId
    }

    private fun getCloudProvider(providerName: String): RemoteMediaProvider? {
        val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return null }
        return providerRegistry.get(providerType) as? RemoteMediaProvider
    }

    override suspend fun <T : Media> toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>,
        favorite: Boolean
    ) {
        val (cloudMedia, localMedia) = mediaList.partition { it.isCloud }
        if (localMedia.isNotEmpty()) {
            repository.toggleFavorite(result, localMedia, favorite)
        }
        if (cloudMedia.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                cloudMedia.forEach { media ->
                    val (providerName, remoteId) = extractCloudInfo(media) ?: return@forEach
                    val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return@forEach }
                    val provider = getCloudProvider(providerName) ?: return@forEach
                    provider.toggleFavorite(remoteId, favorite)
                    cloudMediaDao.updateFavorite(remoteId, providerType, favorite)
                }
            }
        }
    }

    override suspend fun <T : Media> toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>
    ) {
        val turnToFavorite = mediaList.filter { it.favorite == 0 }
        val turnToNotFavorite = mediaList.filter { it.favorite == 1 }
        if (turnToFavorite.isNotEmpty()) {
            toggleFavorite(result, turnToFavorite, true)
        }
        if (turnToNotFavorite.isNotEmpty()) {
            toggleFavorite(result, turnToNotFavorite, false)
        }
    }

    override suspend fun <T : Media> trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>,
        trash: Boolean
    ) = withContext(Dispatchers.Default) {
        val (cloudMedia, localMedia) = mediaList.partition { it.isCloud }

        if (cloudMedia.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                cloudMedia.forEach { media ->
                    val (providerName, remoteId) = extractCloudInfo(media) ?: return@forEach
                    val provider = getCloudProvider(providerName) ?: return@forEach
                    if (trash) {
                        provider.trashAsset(remoteId)
                    } else {
                        provider.restoreAsset(remoteId)
                    }
                }
            }
        }

        if (localMedia.isNotEmpty()) {
            val isTrashEnabled = getTrashEnabled(context).firstOrNull() ?: true
            if ((isTrashEnabled || !trash)) {
                val hasFullAccess = SdkCompat.hasFullFileAccess
                val internalMedia = localMedia.filter { !it.path.matches(sdcardRegex) }
                val sdCardMedia = localMedia.filter { it.path.matches(sdcardRegex) }
                if (internalMedia.isNotEmpty()) {
                    repository.trashMedia(result, internalMedia, trash)
                }
                if (sdCardMedia.isNotEmpty()) {
                    if (hasFullAccess) {
                        repository.trashMediaDirectly(sdCardMedia, trash)
                    } else {
                        repository.deleteMedia(result, sdCardMedia)
                    }
                }
            } else {
                repository.deleteMedia(result, localMedia)
            }
        }
    }

    override suspend fun <T : Media> addMedia(vault: Vault, media: T) {
        workManager.enqueueVaultOperation(
            operation = VaultOperationWorker.OP_ENCRYPT,
            media = listOf(media.getUri()),
            vault = vault
        )
    }

    override fun <T : Media> rotateImage(
        media: T,
        degrees: Int
    ) = workManager.rotateImage(media, degrees)

    override suspend fun <T : Media> copyMedia(
        from: T,
        path: String
    ) = repository.copyMedia(from, path)

    override suspend fun <T : Media> copyMedia(vararg sets: Pair<T, String>) =
        repository.copyMedia(*sets)

    override suspend fun <T : Media> deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<T>
    ) {
        val (cloudMedia, localMedia) = mediaList.partition { it.isCloud }
        if (cloudMedia.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                cloudMedia.forEach { media ->
                    val (providerName, remoteId) = extractCloudInfo(media) ?: return@forEach
                    val providerType = try { ProviderType.valueOf(providerName) } catch (_: Exception) { return@forEach }
                    val provider = getCloudProvider(providerName) ?: return@forEach
                    provider.deleteAsset(remoteId)
                    cloudMediaDao.delete(remoteId, providerType)
                }
            }
        }
        if (localMedia.isNotEmpty()) {
            repository.deleteMedia(result, localMedia)
        }
    }

    override suspend fun <T : Media> renameMedia(
        media: T,
        newName: String
    ): Boolean = repository.renameMedia(media, newName)

    override suspend fun <T : Media> moveMedia(
        media: T,
        newPath: String
    ): Boolean = repository.moveMedia(media, newPath)

    override suspend fun <T : Media> deleteMediaMetadata(
        media: T
    ): Boolean = repository.deleteMediaMetadata(media)

    override suspend fun <T : Media> deleteMediaGPSMetadata(
        media: T
    ): Boolean = repository.deleteMediaGPSMetadata(media)

    override suspend fun <T : Media> updateMediaDescription(
        media: T,
        description: String
    ): Boolean = repository.updateMediaDescription(media, description)

    override suspend fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ): Uri? = repository.saveImage(bitmap, format, mimeType, relativePath, displayName)

    override suspend fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ): Boolean = repository.overrideImage(uri, bitmap, format, mimeType, relativePath, displayName)

    override suspend fun getCategoryForMediaId(mediaId: Long): String? =
        repository.getCategoryForMediaId(mediaId)

    override fun getClassifiedMediaCountAtCategory(category: String): Flow<Int> =
        repository.getClassifiedMediaCountAtCategory(category)

    override fun getClassifiedMediaThumbnailByCategory(category: String): Flow<Media.ClassifiedMedia?> =
        repository.getClassifiedMediaThumbnailByCategory(category)

    override suspend fun deleteAlbumThumbnail(albumId: Long) =
        repository.deleteAlbumThumbnail(albumId)

    override suspend fun updateAlbumThumbnail(albumId: Long, newThumbnail: Uri) =
        repository.updateAlbumThumbnail(albumId, newThumbnail)

    override fun hasAlbumThumbnail(albumId: Long): Flow<Boolean> =
        repository.hasAlbumThumbnail(albumId)

    override suspend fun collectMetadataFor(media: Media) = repository.collectMetadataFor(media)

    override suspend fun <T : Media> downloadCloudMedia(mediaList: List<T>): Result<Int> =
        withContext(Dispatchers.IO) {
            val cloudMedia = mediaList.filter { it.isCloud }
            if (cloudMedia.isEmpty()) return@withContext Result.success(0)

            var successCount = 0
            for (media in cloudMedia) {
                val (providerName, remoteId) = extractCloudInfo(media) ?: continue
                val providerType = try {
                    ProviderType.valueOf(providerName)
                } catch (_: Exception) {
                    continue
                }
                val provider = providerRegistry.get(providerType)
                val syncProvider = provider as? SyncCapableProvider ?: continue

                val downloadResult = syncProvider.downloadAsset(remoteId)
                val cacheUri = downloadResult.getOrNull() ?: continue

                // Save from cache to MediaStore
                try {
                    val isVideo = media.mimeType.startsWith("video/")
                    val collection = if (isVideo) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val relativePath = if (isVideo)
                        Environment.DIRECTORY_MOVIES + "/Cloud"
                    else
                        Environment.DIRECTORY_PICTURES + "/Cloud"

                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, media.label)
                        put(MediaStore.MediaColumns.MIME_TYPE, media.mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val resolver = context.contentResolver
                    val insertUri = resolver.insert(collection, values) ?: continue

                    resolver.openOutputStream(insertUri)?.use { output ->
                        resolver.openInputStream(cacheUri)?.use { input ->
                            input.copyTo(output)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(insertUri, values, null, null)
                    }

                    // Clean up cache file
                    try {
                        resolver.delete(cacheUri, null, null)
                    } catch (_: Exception) {
                        // Cache file may be a plain file, not a content URI
                        java.io.File(cacheUri.path ?: "").delete()
                    }

                    successCount++
                } catch (_: Exception) {
                    continue
                }
            }
            Result.success(successCount)
        }

}