/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import androidx.work.WorkManager
import com.dot.gallery.core.Resource
import com.dot.gallery.core.fileFlowObserver
import com.dot.gallery.core.updateDatabase
import com.dot.gallery.core.util.MediaStoreBuckets
import com.dot.gallery.core.util.ext.copyMedia
import com.dot.gallery.core.util.ext.mapAsResource
import com.dot.gallery.core.util.ext.overrideImage
import com.dot.gallery.core.util.ext.saveImage
import com.dot.gallery.core.util.ext.saveVideo
import com.dot.gallery.core.util.ext.updateMedia
import com.dot.gallery.core.util.ext.updateMediaExif
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.data.data_source.KeychainHolder.Companion.VAULT_INFO_FILE_NAME
import com.dot.gallery.feature_node.data.data_source.mediastore.quries.AlbumsFlow
import com.dot.gallery.feature_node.data.data_source.mediastore.quries.MediaFlow
import com.dot.gallery.feature_node.data.data_source.mediastore.quries.MediaUriFlow
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.DecryptedMedia
import com.dot.gallery.feature_node.domain.model.DecryptedMedia.Companion.asDecryptedMedia
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.model.TimelineSettings
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.compatibleBitmapFormat
import com.dot.gallery.feature_node.domain.model.compatibleMimeType
import com.dot.gallery.feature_node.domain.model.toEncryptedMedia
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.BOTH
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.PHOTOS
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.VIDEOS
import com.dot.gallery.feature_node.presentation.util.printError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable

class MediaRepositoryImpl(
    private val context: Context,
    private val workManager: WorkManager,
    private val database: InternalDatabase,
    private val keychainHolder: KeychainHolder
) : MediaRepository {

    private val contentResolver = context.contentResolver

    override suspend fun updateInternalDatabase() {
        workManager.updateDatabase()
    }

    /**
     * TODO: Add media reordering
     */
    override fun getMedia(): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id
        ).flowData().map {
            Resource.Success(it)
        }.flowOn(Dispatchers.IO)

    override fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = when (allowedMedia) {
                PHOTOS -> MediaStoreBuckets.MEDIA_STORE_BUCKET_PHOTOS.id
                VIDEOS -> MediaStoreBuckets.MEDIA_STORE_BUCKET_VIDEOS.id
                BOTH -> MediaStoreBuckets.MEDIA_STORE_BUCKET_TIMELINE.id
            },
            mimeType = allowedMedia.toStringAny()
        ).flowData().map {
            Resource.Success(it)
        }.flowOn(Dispatchers.IO)

    override fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = MediaStoreBuckets.MEDIA_STORE_BUCKET_FAVORITES.id
        ).flowData().map {
            Resource.Success(it)
        }.flowOn(Dispatchers.IO)

    override fun getTrashed(): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = MediaStoreBuckets.MEDIA_STORE_BUCKET_TRASH.id
        ).flowData().map { Resource.Success(it) }.flowOn(Dispatchers.IO)

    override fun getAlbums(mediaOrder: MediaOrder): Flow<Resource<List<Album>>> =
        AlbumsFlow(context).flowData().map {
            withContext(Dispatchers.IO) {
                val data = it.toMutableList().apply {
                    replaceAll { album ->
                        album.copy(isPinned = database.getPinnedDao().albumIsPinned(album.id))
                    }
                }

                Resource.Success(mediaOrder.sortAlbums(data))
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().insertPinnedAlbum(pinnedAlbum)

    override suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().removePinnedAlbum(pinnedAlbum)

    override fun getPinnedAlbums(): Flow<List<PinnedAlbum>> =
        database.getPinnedDao().getPinnedAlbums()

    override suspend fun addBlacklistedAlbum(ignoredAlbum: IgnoredAlbum) =
        database.getBlacklistDao().addBlacklistedAlbum(ignoredAlbum)

    override suspend fun removeBlacklistedAlbum(ignoredAlbum: IgnoredAlbum) =
        database.getBlacklistDao().removeBlacklistedAlbum(ignoredAlbum)

    override fun getBlacklistedAlbums(): Flow<List<IgnoredAlbum>> =
        database.getBlacklistDao().getBlacklistedAlbums()

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = albumId,
        ).flowData().mapAsResource()

    override fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<Media>>> =
        MediaFlow(
            contentResolver = contentResolver,
            buckedId = albumId,
            mimeType = allowedMedia.toStringAny()
        ).flowData().mapAsResource()

    override fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>> =
        AlbumsFlow(
            context = context,
            mimeType = allowedMedia.toStringAny()
        ).flowData().mapAsResource()

    override fun getMediaListByUris(
        listOfUris: List<Uri>,
        reviewMode: Boolean
    ): Flow<Resource<List<Media>>> =
        MediaUriFlow(
            contentResolver = contentResolver,
            uris = listOfUris,
            reviewMode = reviewMode
        ).flowData().mapAsResource(errorOnEmpty = true, errorMessage = "Media could not be opened")

    override suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    ) {
        val intentSender = MediaStore.createFavoriteRequest(
            contentResolver,
            mediaList.map { it.uri },
            favorite
        ).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
    ) {
        val intentSender = MediaStore.createTrashRequest(
            contentResolver,
            mediaList.map { it.uri },
            trash
        ).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest, ActivityOptionsCompat.makeTaskLaunchBehind())
    }

    override suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    ) {
        val intentSender =
            MediaStore.createDeleteRequest(
                contentResolver,
                mediaList.map { it.uri }).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun copyMedia(
        from: Media,
        path: String
    ): Boolean = contentResolver.copyMedia(
        from = from,
        path = path
    )

    override suspend fun renameMedia(
        media: Media,
        newName: String
    ): Boolean = contentResolver.updateMedia(
        media = media,
        contentValues = displayName(newName)
    )

    override suspend fun moveMedia(
        media: Media,
        newPath: String
    ): Boolean = contentResolver.updateMedia(
        media = media,
        contentValues = relativePath(newPath)
    )

    override suspend fun updateMediaExif(
        media: Media,
        exifAttributes: ExifAttributes
    ): Boolean = contentResolver.updateMediaExif(
        media = media,
        exifAttributes = exifAttributes
    )

    override fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = contentResolver.saveImage(bitmap, format, mimeType, relativePath, displayName)

    override fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = contentResolver.overrideImage(uri, bitmap, format, mimeType, relativePath, displayName)

    override fun getVaults(): Flow<Resource<List<Vault>>> =
        context.retrieveInternalFiles {
            with(keychainHolder) {
                filesDir.listFiles()
                    ?.filter { it.isDirectory && File(it, VAULT_INFO_FILE_NAME).exists() }
                    ?.mapNotNull {
                        try {
                            File(it, VAULT_INFO_FILE_NAME).decrypt() as Vault
                        } catch (e: Exception) {
                            null
                        }
                    }
                    ?: emptyList()
            }
        }


    override suspend fun createVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) = withContext(Dispatchers.IO) { keychainHolder.writeVaultInfo(vault, onSuccess, onFailed) }

    override suspend fun deleteVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) = withContext(Dispatchers.IO) { keychainHolder.deleteVault(vault, onSuccess, onFailed) }

    override fun getEncryptedMedia(vault: Vault): Flow<Resource<List<DecryptedMedia>>> =
        context.retrieveInternalFiles {
            with(keychainHolder) {
                withContext(Dispatchers.IO) {
                    (vaultFolder(vault).listFiles()?.filter { it.name.endsWith("enc") }
                        ?: emptyList()).map { file ->
                        try {
                            val id = file.nameWithoutExtension.toLong()
                            val encryptedFile = vault.mediaFile(id)
                            val encryptedMedia = encryptedFile.decrypt<EncryptedMedia>()
                            encryptedMedia.asDecryptedMedia(Uri.fromFile(encryptedFile))
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.filterNotNull().sortedByDescending { it.timestamp }
            }
        }

    override suspend fun addMedia(vault: Vault, media: Media): Boolean =
        withContext(Dispatchers.IO) {
            with(keychainHolder) {
                keychainHolder.checkVaultFolder(vault)
                val output = vault.mediaFile(media.id)
                if (output.exists()) output.delete()
                val encryptedMedia =
                    getBytes(media.uri)?.let { bytes -> media.toEncryptedMedia(bytes) }
                return@withContext try {
                    encryptedMedia?.let {
                        output.encrypt(it)
                    }
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    printError("Failed to add file: ${media.label}")
                    false
                }
            }
        }

    override suspend fun restoreMedia(vault: Vault, media: DecryptedMedia): Boolean =
        withContext(Dispatchers.IO) {
            with(keychainHolder) {
                checkVaultFolder(vault)
                return@withContext try {
                    val output = vault.mediaFile(media.id)
                    val encryptedMedia = output.decrypt<EncryptedMedia>()
                    val restored: Boolean
                    if (media.isImage) {
                        restored = saveImage(
                            bitmap = BitmapFactory.decodeByteArray(encryptedMedia.bytes, 0, encryptedMedia.bytes.size),
                            displayName = media.label,
                            mimeType = media.compatibleMimeType(),
                            format = media.compatibleBitmapFormat(),
                            relativePath = Environment.DIRECTORY_PICTURES + "/Restored"
                        ) != null
                    } else {
                        restored = contentResolver.saveVideo(
                            bytes = encryptedMedia.bytes,
                            displayName = media.label,
                            mimeType = media.compatibleMimeType(),
                            relativePath = Environment.DIRECTORY_MOVIES + "/Restored"
                        ) != null
                    }
                    val deleted = if (restored) output.delete() else false
                    restored && deleted
                } catch (e: Exception) {
                    e.printStackTrace()
                    printError("Failed to restore file: ${media.label}")
                    false
                }
            }
        }

    override suspend fun deleteEncryptedMedia(vault: Vault, media: DecryptedMedia): Boolean =
        withContext(Dispatchers.IO) {
            with(keychainHolder) {
                checkVaultFolder(vault)
                return@withContext try {
                    vault.mediaFile(media.id).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    printError("Failed to delete file: ${media.label}")
                    false
                }
            }
        }

    override suspend fun deleteAllEncryptedMedia(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (failedFiles: List<File>) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        with(keychainHolder) {
            checkVaultFolder(vault)
            val failedFiles = mutableListOf<File>()
            val files = vaultFolder(vault).listFiles()
            files?.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                    printError("Failed to delete file: ${file.name}")
                    failedFiles.add(file)
                }
            }
            if (failedFiles.isEmpty()) {
                onSuccess()
                true
            } else {
                onFailed(failedFiles)
                false
            }
        }
    }

    override fun getSettings(): Flow<TimelineSettings?> = database.getMediaDao().getTimelineSettings()
    override suspend fun updateSettings(settings: TimelineSettings) {
        database.getMediaDao().setTimelineSettings(settings)
    }

    companion object {
        private fun displayName(newName: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        private fun relativePath(newPath: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, newPath)
        }

        private fun <T : Serializable> Context.retrieveInternalFiles(dataBody: suspend (ContentResolver) -> List<T>) =
            fileFlowObserver().map {
                try {
                    Resource.Success(data = dataBody.invoke(contentResolver))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()
    }
}