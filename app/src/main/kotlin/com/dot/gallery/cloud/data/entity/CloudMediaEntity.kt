/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.data.entity

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.SyncState
import com.dot.gallery.core.Constants
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.util.getDate

@Entity(
    tableName = "cloud_media",
    primaryKeys = ["remoteId", "providerType"],
    indices = [
        Index(value = ["serverConfigId"]),
        Index(value = ["syncState"]),
        Index(value = ["timestamp"]),
        Index(value = ["favorite"]),
        Index(value = ["trashed"]),
        Index(value = ["contentHash"])
    ]
)
data class CloudMediaEntity(
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val label: String = "",
    val path: String = "",
    val relativePath: String = "",
    val mimeType: String = "",
    val timestamp: Long = 0L,
    val takenTimestamp: Long? = null,
    val size: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val duration: String? = null,
    val favorite: Boolean = false,
    val trashed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
    val syncState: SyncState = SyncState.REMOTE_ONLY,
    @ColumnInfo(defaultValue = "")
    val localCopyPath: String = "",
    val contentHash: String? = null,
    val thumbnailUrl: String = "",
    val originalUrl: String = "",
    val lastSyncedAt: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String? = null,
    @ColumnInfo(name = "state_name")
    val state: String? = null,
    val country: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    @ColumnInfo(defaultValue = "")
    val lensModel: String? = null,
    @ColumnInfo(defaultValue = "")
    val imageDescription: String? = null,
    val dateTimeOriginal: String? = null,
    val exposureTime: String? = null,
    val aperture: String? = null,
    val iso: Int? = null,
    val focalLength: Double? = null
) {
    fun toUriMedia(): Media.UriMedia {
        val cloudUri = "cloud://${providerType.name}/$remoteId?size=preview".toUri()
        // timestamp → seconds (MediaStore DATE_MODIFIED convention)
        val timestampSeconds = timestamp / 1000L
        // takenTimestamp stays in millis (MediaStore DATE_TAKEN convention)
        // Media.definedTimestamp will divide by 1000 when needed
        val displayName = providerType.displayName
        val displayPath = if (path.isNotBlank()) "$displayName/$path" else "$displayName/$label"
        // Use takenTimestamp (millis/1000) for display date if available, else fallback to timestamp seconds
        val displayDateSeconds = takenTimestamp?.let { it / 1000L } ?: timestampSeconds
        return Media.UriMedia(
            id = com.dot.gallery.cloud.core.stableIdHash(remoteId),
            label = label,
            uri = cloudUri,
            path = displayPath,
            relativePath = if (relativePath.isNotBlank()) "$displayName/$relativePath" else displayName,
            albumID = CLOUD_ALBUM_ID,
            albumLabel = displayName,
            timestamp = timestampSeconds,
            takenTimestamp = takenTimestamp,
            fullDate = displayDateSeconds.getDate(Constants.EXTENDED_DATE_FORMAT),
            mimeType = mimeType,
            favorite = if (favorite) 1 else 0,
            trashed = if (trashed) 1 else 0,
            size = size,
            duration = duration
        )
    }

    companion object {
        const val CLOUD_ALBUM_ID = -500L
    }
}
