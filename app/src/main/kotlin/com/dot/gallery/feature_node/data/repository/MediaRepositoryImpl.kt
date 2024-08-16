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
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityOptionsCompat
import com.dot.gallery.core.Resource
import com.dot.gallery.core.contentFlowObserver
import com.dot.gallery.core.contentFlowWithDatabase
import com.dot.gallery.core.fileFlowObserver
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.data_source.KeychainHolder
import com.dot.gallery.feature_node.data.data_source.KeychainHolder.Companion.VAULT_INFO_FILE_NAME
import com.dot.gallery.feature_node.data.data_source.Query
import com.dot.gallery.feature_node.data.data_types.copyMedia
import com.dot.gallery.feature_node.data.data_types.findMedia
import com.dot.gallery.feature_node.data.data_types.getAlbums
import com.dot.gallery.feature_node.data.data_types.getMedia
import com.dot.gallery.feature_node.data.data_types.getMediaByUri
import com.dot.gallery.feature_node.data.data_types.getMediaFavorite
import com.dot.gallery.feature_node.data.data_types.getMediaListByUris
import com.dot.gallery.feature_node.data.data_types.getMediaTrashed
import com.dot.gallery.feature_node.data.data_types.overrideImage
import com.dot.gallery.feature_node.data.data_types.saveImage
import com.dot.gallery.feature_node.data.data_types.updateMedia
import com.dot.gallery.feature_node.data.data_types.updateMediaExif
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.IgnoredAlbum
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.model.toEncryptedMedia
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.domain.util.OrderType
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.BOTH
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.PHOTOS
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia.VIDEOS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.Serializable

class MediaRepositoryImpl(
    private val context: Context,
    private val database: InternalDatabase,
    private val keychainHolder: KeychainHolder
) : MediaRepository {

    /**
     * TODO: Add media reordering
     */
    override fun getMedia(): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            it.getMedia(mediaOrder = DEFAULT_ORDER).removeBlacklisted()
        }

    override fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            val query = when (allowedMedia) {
                PHOTOS -> Query.PhotoQuery()
                VIDEOS -> Query.VideoQuery()
                BOTH -> Query.MediaQuery()
            }
            it.getMedia(mediaQuery = query, mediaOrder = DEFAULT_ORDER).removeBlacklisted()
        }

    override fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            it.getMediaFavorite(mediaOrder = mediaOrder).removeBlacklisted()
        }

    override fun getTrashed(): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            it.getMediaTrashed().removeBlacklisted()
        }

    override fun getAlbums(
        mediaOrder: MediaOrder,
        ignoreBlacklisted: Boolean
    ): Flow<Resource<List<Album>>> =
        context.retrieveAlbums(database) { cr ->
            cr.getAlbums(mediaOrder = mediaOrder).toMutableList().apply {
                replaceAll { album ->
                    album.copy(isPinned = database.getPinnedDao().albumIsPinned(album.id))
                }
            }.removeBlacklisted(ignoreBlacklisted)
        }

    override suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().insertPinnedAlbum(pinnedAlbum)

    override suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum) =
        database.getPinnedDao().removePinnedAlbum(pinnedAlbum)

    override suspend fun addBlacklistedAlbum(ignoredAlbum: IgnoredAlbum) =
        database.getBlacklistDao().addBlacklistedAlbum(ignoredAlbum)

    override suspend fun removeBlacklistedAlbum(ignoredAlbum: IgnoredAlbum) =
        database.getBlacklistDao().removeBlacklistedAlbum(ignoredAlbum)

    override fun getBlacklistedAlbums(): Flow<List<IgnoredAlbum>> =
        database.getBlacklistDao().getBlacklistedAlbums()

    override suspend fun getMediaById(mediaId: Long): Media? {
        val query = Query.MediaQuery().copy(
            bundle = Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    MediaStore.MediaColumns._ID + "= ?"
                )
                putStringArray(
                    ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                    arrayOf(mediaId.toString())
                )
            }
        )
        return context.contentResolver.findMedia(query)
    }

    override fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            val query = Query.MediaQuery().copy(
                bundle = Bundle().apply {
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.BUCKET_ID + "= ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(albumId.toString())
                    )
                }
            )
            /** return@retrieveMedia */
            it.getMedia(query)
        }

    override fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<Media>>> =
        context.retrieveMedia(database) {
            val query = Query.MediaQuery().copy(
                bundle = Bundle().apply {
                    val mimeType = when (allowedMedia) {
                        PHOTOS -> "image%"
                        VIDEOS -> "video%"
                        BOTH -> "%/%"
                    }
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.BUCKET_ID + "= ? and " + MediaStore.MediaColumns.MIME_TYPE + " like ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(albumId.toString(), mimeType)
                    )
                }
            )
            /** return@retrieveMedia */
            it.getMedia(query)
        }

    override fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>> =
        context.retrieveAlbums(database) {
            val query = Query.AlbumQuery().copy(
                bundle = Bundle().apply {
                    val mimeType = when (allowedMedia) {
                        PHOTOS -> "image%"
                        VIDEOS -> "video%"
                        BOTH -> "%/%"
                    }
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        MediaStore.MediaColumns.MIME_TYPE + " like ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(mimeType)
                    )
                }
            )
            it.getAlbums(query, mediaOrder = MediaOrder.Label(OrderType.Ascending))
        }

    override fun getMediaByUri(
        uriAsString: String,
        isSecure: Boolean
    ): Flow<Resource<List<Media>>> =
        context.retrieveMediaAsResource {
            val media = context.getMediaByUri(Uri.parse(uriAsString))
            /** return@retrieveMediaAsResource */
            if (media == null) {
                Resource.Error(message = "Media could not be opened")
            } else {
                val query = Query.MediaQuery().copy(
                    bundle = Bundle().apply {
                        putString(
                            ContentResolver.QUERY_ARG_SQL_SELECTION,
                            MediaStore.MediaColumns.BUCKET_ID + "= ?"
                        )
                        putStringArray(
                            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                            arrayOf(media.albumID.toString())
                        )
                    }
                )
                Resource.Success(
                    data = if (isSecure) listOf(media) else it.getMedia(query)
                        .ifEmpty { listOf(media) })
            }
        }

    override fun getMediaListByUris(
        listOfUris: List<Uri>,
        reviewMode: Boolean
    ): Flow<Resource<List<Media>>> =
        context.retrieveMediaAsResource {
            var mediaList = context.getMediaListByUris(listOfUris)
            if (reviewMode) {
                val query = Query.MediaQuery().copy(
                    bundle = Bundle().apply {
                        putString(
                            ContentResolver.QUERY_ARG_SQL_SELECTION,
                            MediaStore.MediaColumns.BUCKET_ID + "= ?"
                        )
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
                        putStringArray(
                            ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                            arrayOf(mediaList.first().albumID.toString())
                        )
                    }
                )
                mediaList = it.getMedia(query).filter { media -> !media.isTrashed }
            }
            if (mediaList.isEmpty()) {
                Resource.Error(message = "Media could not be opened")
            } else {
                Resource.Success(data = mediaList)
            }
        }

    override suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    ) {
        val intentSender = MediaStore.createFavoriteRequest(
            context.contentResolver,
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
            context.contentResolver,
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
                context.contentResolver,
                mediaList.map { it.uri }).intentSender
        val senderRequest: IntentSenderRequest = IntentSenderRequest.Builder(intentSender)
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
            .build()
        result.launch(senderRequest)
    }

    override suspend fun copyMedia(
        from: Media,
        path: String
    ): Boolean = context.contentResolver.copyMedia(
        from = from,
        path = path
    )

    override suspend fun renameMedia(
        media: Media,
        newName: String
    ): Boolean = context.contentResolver.updateMedia(
        media = media,
        contentValues = displayName(newName)
    )

    override suspend fun moveMedia(
        media: Media,
        newPath: String
    ): Boolean = context.contentResolver.updateMedia(
        media = media,
        contentValues = relativePath(newPath)
    )

    override suspend fun updateMediaExif(
        media: Media,
        exifAttributes: ExifAttributes
    ): Boolean = context.contentResolver.updateMediaExif(
        media = media,
        exifAttributes = exifAttributes
    )

    override fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = context.contentResolver.saveImage(bitmap, format, mimeType, relativePath, displayName)

    override fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ) = context.contentResolver.overrideImage(uri, bitmap, format, mimeType, relativePath, displayName)

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
    ) {
        var collected = false
        getVaults().collectLatest {
            if (!collected) {
                collected = true
                val alreadyExists = it.data?.contains(vault) ?: false
                if (alreadyExists) {
                    onFailed("Vault \"${vault.name}\" exists")
                } else {
                    keychainHolder.writeVaultInfo(vault, onSuccess)
                }
            }
        }
    }

    override suspend fun deleteVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    ) {
        try {
            with(keychainHolder) { vaultFolder(vault).deleteRecursively() }
        } catch (e: IOException) {
            e.printStackTrace()
            onFailed("Failed to delete vault ${vault.name} (${vault.uuid})")
            return
        }
        onSuccess()
    }

    override fun getEncryptedMedia(vault: Vault): Flow<Resource<List<EncryptedMedia>>> =
        context.retrieveInternalFiles {
            with(keychainHolder) {
                vaultFolder(vault).listFiles()?.filter {
                    it.name.endsWith("enc")
                }?.mapNotNull {
                    try {
                        val id = it.nameWithoutExtension.toLong()
                        vault.mediaFile(id).decrypt<EncryptedMedia>()
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
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
                    false
                }
            }
        }

    override suspend fun restoreMedia(vault: Vault, media: EncryptedMedia): Boolean =
        withContext(Dispatchers.IO) {
            with(keychainHolder) {
                checkVaultFolder(vault)
                return@withContext try {
                    val output = vault.mediaFile(media.id)
                    val bitmap = BitmapFactory.decodeByteArray(media.bytes, 0, media.bytes.size)
                    val restored = saveImage(
                        bitmap = bitmap,
                        displayName = media.label,
                        mimeType = "image/png",
                        format = Bitmap.CompressFormat.PNG,
                        relativePath = Environment.DIRECTORY_PICTURES + "/Restored"
                    ) != null
                    val deleted = if (restored) output.delete() else false
                    restored && deleted
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }

    override suspend fun deleteEncryptedMedia(vault: Vault, media: EncryptedMedia): Boolean =
        withContext(Dispatchers.IO) {
            with(keychainHolder) {
                checkVaultFolder(vault)
                return@withContext try {
                    vault.mediaFile(media.id).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
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

    private fun List<Media>.removeBlacklisted(): List<Media> = toMutableList().apply {
        val blacklistedAlbums = database.getBlacklistDao().getBlacklistedAlbumsSync()
        removeAll { media -> blacklistedAlbums.any {  it.matchesMedia(media) } }
    }

    private fun MutableList<Album>.removeBlacklisted(ignoreBlacklisted: Boolean): List<Album> = apply {
        if (!ignoreBlacklisted) {
            val blacklistedAlbums = database.getBlacklistDao().getBlacklistedAlbumsSync()
            removeAll { album -> blacklistedAlbums.any { it.matchesAlbum(album) } }
        }
    }

    companion object {
        private val DEFAULT_ORDER = MediaOrder.Date(OrderType.Descending)
        private val URIs = arrayOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )

        private fun displayName(newName: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
        }

        private fun relativePath(newPath: String) = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, newPath)
        }

        private fun Context.retrieveMediaAsResource(dataBody: suspend (ContentResolver) -> Resource<List<Media>>) =
            contentFlowObserver(URIs).map {
                try {
                    dataBody.invoke(contentResolver)
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()

        private fun Context.retrieveMedia(database: InternalDatabase, dataBody: suspend (ContentResolver) -> List<Media>) =
            contentFlowWithDatabase(URIs, database).map {
                try {
                    Resource.Success(data = dataBody.invoke(contentResolver))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()

        private fun Context.retrieveAlbums(database: InternalDatabase, dataBody: suspend (ContentResolver) -> List<Album>) =
            contentFlowWithDatabase(URIs, database).map {
                try {
                    Resource.Success(data = dataBody.invoke(contentResolver))
                } catch (e: Exception) {
                    Resource.Error(message = e.localizedMessage ?: "An error occurred")
                }
            }.conflate()

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