/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import android.net.Uri
import androidx.core.net.toUri
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.Media
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class SyncState {
    REMOTE_ONLY,
    DOWNLOADING,
    SYNCED,
    UPLOAD_PENDING,
    CONFLICT
}

enum class ThumbnailSize {
    THUMBNAIL,
    PREVIEW
}

@Serializable
data class CloudAlbum(
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val name: String,
    val assetCount: Int,
    val thumbnailAssetId: String? = null,
    val isShared: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncEnabled: Boolean = true
) {
    fun toAlbum(): Album {
        val thumbUri = if (thumbnailAssetId != null) {
            "cloud://${providerType.name}/$thumbnailAssetId?size=thumbnail".toUri()
        } else Uri.EMPTY
        return Album(
            id = CLOUD_ALBUM_ID_BASE - stableIdHash(remoteId),
            label = "$name (${providerType.displayName})",
            uri = thumbUri,
            pathToThumbnail = thumbUri.toString(),
            relativePath = "cloud/${providerType.name}",
            timestamp = updatedAt / 1000L,
            count = assetCount.toLong(),
            size = 0L
        )
    }

    companion object {
        const val CLOUD_ALBUM_ID_BASE = -1000L
    }
}

@Serializable
data class CloudStorageInfo(
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val usedPercentage: Double = 0.0,
    val usedFormatted: String = "",
    val totalFormatted: String = ""
)

@Serializable
data class CloudServerConfig(
    val id: Long = 0L,
    val providerType: ProviderType,
    val serverUrl: String,
    val apiKey: String? = null,
    val username: String? = null,
    val password: String? = null,
    val displayName: String = "",
    val isActive: Boolean = true,
    val lastConnected: Long = 0L,
    val syncEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val syncIntervalMinutes: Int = 360,
    // Backup options
    val cellularPhotos: Boolean = false,
    val cellularVideos: Boolean = false,
    val requireCharging: Boolean = false,
    val syncAlbums: Boolean = false,
    // Notification settings
    val showBackupTotalProgress: Boolean = true,
    val showBackupDetailProgress: Boolean = false,
    val notifyBackupFailures: Boolean = true,
    // Networking
    val autoUrlSwitch: Boolean = false,
    val localWifiSsid: String = "",
    val localServerUrl: String = "",
    val externalUrls: String = "[]",
    // Viewer settings
    val loadPreviewImage: Boolean = true,
    val loadOriginalImage: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val loopVideos: Boolean = false,
    val forceOriginalVideo: Boolean = false,
    // Advanced settings
    val verboseLogging: Boolean = false,
    val syncRemoteDeletions: Boolean = false,
    val preferRemoteImages: Boolean = false,
    val readOnlyMode: Boolean = false
)

@Serializable
data class CloudServerInfo(
    val version: String,
    val serverName: String,
    val storageUsed: Long = 0L,
    val storageTotal: Long = 0L
)

@Serializable
data class CloudAuthToken(
    val accessToken: String,
    val userId: String? = null,
    val userEmail: String? = null,
    val isAdmin: Boolean = false
)

@Serializable
data class CloudMapMarker(
    val latitude: Double,
    val longitude: Double,
    val assetId: String,
    val providerType: ProviderType,
    val city: String? = null,
    val country: String? = null
)

@Serializable
data class PersonInfo(
    val id: String,
    val name: String,
    val providerType: ProviderType,
    val thumbnailUrl: String? = null,
    val assetCount: Int = 0,
    val birthDate: String? = null
)

@Serializable
data class DetectedFace(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val embedding: FloatArray? = null,
    val confidence: Float = 0f,
    val personId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DetectedFace) return false
        return left == other.left && top == other.top &&
                right == other.right && bottom == other.bottom &&
                personId == other.personId
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class SharedLinkInfo(
    val id: String,
    val key: String,
    val type: String = "INDIVIDUAL",
    val description: String? = null,
    val expiresAt: Long? = null,
    val allowDownload: Boolean = true,
    val allowUpload: Boolean = false,
    val showMetadata: Boolean = true,
    val password: String? = null,
    val assetCount: Int = 0,
    val providerType: ProviderType,
    val createdAt: Long = 0L,
    val thumbnailAssetId: String? = null,
    val albumId: String? = null,
    val albumName: String? = null
) {
    fun shareUrl(baseUrl: String): String = "$baseUrl/share/$key"

    val displayTitle: String
        get() = when {
            type == "ALBUM" && !albumName.isNullOrBlank() -> albumName
            !description.isNullOrBlank() -> description
            else -> "$assetCount items"
        }

    val isAlbumLink: Boolean get() = type == "ALBUM"
}

@Serializable
data class MemoryInfo(
    val id: String,
    val type: String = "on_this_day",
    val year: Int = 0,
    val assetCount: Int = 0,
    val providerType: ProviderType,
    val createdAt: Long = 0L,
    val seenAt: Long? = null,
    @Transient
    val media: List<Media.UriMedia> = emptyList()
)

@Serializable
data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList(),
    val providerType: ProviderType
)

@Serializable
data class OcrBlock(
    val text: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 0f
)
