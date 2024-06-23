/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.dot.gallery.core.Resource
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.BlacklistedAlbum
import com.dot.gallery.feature_node.domain.model.EncryptedMedia
import com.dot.gallery.feature_node.domain.model.ExifAttributes
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.domain.model.PinnedAlbum
import com.dot.gallery.feature_node.domain.model.Vault
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.picker.AllowedMedia
import kotlinx.coroutines.flow.Flow
import java.io.File

interface MediaRepository {

    fun getMedia(): Flow<Resource<List<Media>>>

    fun getMediaByType(allowedMedia: AllowedMedia): Flow<Resource<List<Media>>>

    fun getFavorites(mediaOrder: MediaOrder): Flow<Resource<List<Media>>>

    fun getTrashed(): Flow<Resource<List<Media>>>

    fun getAlbums(
        mediaOrder: MediaOrder,
        ignoreBlacklisted: Boolean = false
    ): Flow<Resource<List<Album>>>

    suspend fun insertPinnedAlbum(pinnedAlbum: PinnedAlbum)

    suspend fun removePinnedAlbum(pinnedAlbum: PinnedAlbum)

    suspend fun addBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum)

    suspend fun removeBlacklistedAlbum(blacklistedAlbum: BlacklistedAlbum)

    fun getBlacklistedAlbums(): Flow<List<BlacklistedAlbum>>

    suspend fun getMediaById(mediaId: Long): Media?

    fun getMediaByAlbumId(albumId: Long): Flow<Resource<List<Media>>>

    fun getMediaByAlbumIdWithType(
        albumId: Long,
        allowedMedia: AllowedMedia
    ): Flow<Resource<List<Media>>>

    fun getAlbumsWithType(allowedMedia: AllowedMedia): Flow<Resource<List<Album>>>

    fun getMediaByUri(uriAsString: String, isSecure: Boolean): Flow<Resource<List<Media>>>

    fun getMediaListByUris(listOfUris: List<Uri>, reviewMode: Boolean): Flow<Resource<List<Media>>>

    suspend fun toggleFavorite(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        favorite: Boolean
    )

    suspend fun trashMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>,
        trash: Boolean
    )

    suspend fun copyMedia(
        from: Media,
        path: String
    ): Boolean

    suspend fun deleteMedia(
        result: ActivityResultLauncher<IntentSenderRequest>,
        mediaList: List<Media>
    )

    suspend fun renameMedia(
        media: Media,
        newName: String
    ): Boolean

    suspend fun moveMedia(
        media: Media,
        newPath: String
    ): Boolean

    suspend fun updateMediaExif(
        media: Media,
        exifAttributes: ExifAttributes
    ): Boolean

    fun saveImage(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ): Uri?

    fun overrideImage(
        uri: Uri,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        mimeType: String,
        relativePath: String,
        displayName: String
    ): Boolean

    fun getVaults(): Flow<Resource<List<Vault>>>

    suspend fun createVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    )

    suspend fun deleteVault(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (reason: String) -> Unit
    )

    fun getEncryptedMedia(vault: Vault): Flow<Resource<List<EncryptedMedia>>>

    suspend fun addMedia(vault: Vault, media: Media): Boolean

    suspend fun restoreMedia(vault: Vault, media: EncryptedMedia): Boolean

    suspend fun deleteEncryptedMedia(vault: Vault, media: EncryptedMedia): Boolean

    suspend fun deleteAllEncryptedMedia(
        vault: Vault,
        onSuccess: () -> Unit,
        onFailed: (failedFiles: List<File>) -> Unit
    ): Boolean

}