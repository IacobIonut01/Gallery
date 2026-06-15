/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/**
 * Top-level manifest describing the contents of a ReFra backup archive.
 *
 * The backup is a ZIP archive containing this manifest (as `manifest.json`) plus,
 * for each vault, the fully decrypted media files under `vaults/<uuid>/<mediaId>`.
 */
@Serializable
data class BackupManifest(
    val schemaVersion: Int = SCHEMA_VERSION,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val exportedAt: Long = 0L,
    val settings: Map<String, SettingValue> = emptyMap(),
    val localFavorites: List<LocalFavoriteEntry> = emptyList(),
    val cloudFavorites: List<CloudFavoriteEntry> = emptyList(),
    val cloudConfigs: List<CloudConfigEntry> = emptyList(),
    val vaults: List<VaultEntry> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val MANIFEST_NAME = "manifest.json"
        const val VAULTS_DIR = "vaults"
    }
}

/**
 * A single DataStore preference value, tagged with its primitive type so it can be
 * faithfully reconstructed on import.
 *
 * [type] codes: b=Boolean, i=Int, l=Long, f=Float, d=Double, s=String, ss=Set<String>.
 * For `ss`, [value] holds a JSON-encoded list of strings.
 */
@Serializable
data class SettingValue(
    val type: String,
    val value: String
)

/**
 * A local (MediaStore) favorite. Local favorites live in the system MediaStore
 * (IS_FAVORITE) and are not stored by the app, so they are matched back to device
 * media on import by relative path + display name (with a size fallback).
 */
@Serializable
data class LocalFavoriteEntry(
    val displayName: String,
    val relativePath: String,
    val path: String,
    val size: Long,
    val timestamp: Long
)

/** A cloud favorite, identified by its provider and remote id. */
@Serializable
data class CloudFavoriteEntry(
    val providerType: String,
    val remoteId: String
)

/** A cloud server configuration. Mirrors [com.dot.gallery.cloud.data.entity.CloudServerConfigEntity]. */
@Serializable
data class CloudConfigEntry(
    val providerType: String,
    val serverUrl: String,
    val apiKey: String? = null,
    val username: String? = null,
    val encryptedPassword: String? = null,
    val displayName: String = "",
    val isActive: Boolean = true,
    val lastConnected: Long = 0L,
    val syncEnabled: Boolean = false,
    val wifiOnly: Boolean = true,
    val syncIntervalMinutes: Int = 360,
    val syncFolders: String = "",
    val cellularPhotos: Boolean = false,
    val cellularVideos: Boolean = false,
    val requireCharging: Boolean = false,
    val syncAlbums: Boolean = false,
    val showBackupTotalProgress: Boolean = true,
    val showBackupDetailProgress: Boolean = false,
    val notifyBackupFailures: Boolean = true,
    val autoUrlSwitch: Boolean = false,
    val localWifiSsid: String = "",
    val localServerUrl: String = "",
    val externalUrls: String = "[]",
    val loadPreviewImage: Boolean = true,
    val loadOriginalImage: Boolean = false,
    val autoPlayVideos: Boolean = true,
    val loopVideos: Boolean = false,
    val forceOriginalVideo: Boolean = false,
    val verboseLogging: Boolean = false,
    val syncRemoteDeletions: Boolean = false,
    val preferRemoteImages: Boolean = false,
    val readOnlyMode: Boolean = false
)

/** A vault and the metadata of all media it contains. Binary content is stored separately in the archive. */
@Serializable
data class VaultEntry(
    val uuid: String,
    val name: String,
    val media: List<VaultMediaEntry> = emptyList()
)

/**
 * Metadata of a single vault media item. Mirrors [com.dot.gallery.feature_node.domain.model.Media.EncryptedMedia2].
 * [fileName] is the archive entry path that holds the decrypted binary content.
 */
@Serializable
data class VaultMediaEntry(
    val id: Long,
    val label: String,
    val path: String,
    val relativePath: String,
    val albumID: Long,
    val albumLabel: String,
    val timestamp: Long,
    val expiryTimestamp: Long? = null,
    val takenTimestamp: Long? = null,
    val fullDate: String,
    val mimeType: String,
    val favorite: Int,
    val trashed: Int,
    val size: Long,
    val duration: String? = null,
    val fileName: String
)

/**
 * Selectable categories of data to include in an export / apply on import.
 *
 * [selectedVaultIds] further narrows the VAULTS section to specific vault UUIDs.
 * `null` means "all vaults" (the default); a non-null set restricts to those ids only.
 */
data class BackupSelection(
    val settings: Boolean = true,
    val localFavorites: Boolean = true,
    val cloudFavorites: Boolean = true,
    val cloudConfigs: Boolean = true,
    val vaults: Boolean = true,
    val selectedVaultIds: Set<String>? = null
) {
    /** True if the vault with [uuid] should be included (all vaults when no explicit set). */
    fun isVaultSelected(uuid: String): Boolean = selectedVaultIds?.contains(uuid) ?: true
}

/** The individual data categories handled during a backup / restore, used for per-section progress. */
enum class BackupSection {
    SETTINGS,
    LOCAL_FAVORITES,
    CLOUD_FAVORITES,
    CLOUD_CONFIGS,
    VAULTS
}

/**
 * Summary of what a backup archive contains, produced by inspecting the manifest only
 * (without restoring anything). Used to drive the import "pick what to restore" step.
 */
data class BackupContents(
    val schemaVersion: Int,
    val appVersionName: String,
    val exportedAt: Long,
    val encrypted: Boolean,
    val settingsCount: Int,
    val localFavoritesCount: Int,
    val cloudFavoritesCount: Int,
    val cloudConfigsCount: Int,
    val vaultCount: Int,
    val vaultMediaCount: Int,
    /** Per-vault summary so the import UI can let the user pick individual vaults. */
    val vaults: List<VaultSummary> = emptyList(),
) {
    /** True if the given section has any data in this backup. */
    fun has(section: BackupSection): Boolean = when (section) {
        BackupSection.SETTINGS -> settingsCount > 0
        BackupSection.LOCAL_FAVORITES -> localFavoritesCount > 0
        BackupSection.CLOUD_FAVORITES -> cloudFavoritesCount > 0
        BackupSection.CLOUD_CONFIGS -> cloudConfigsCount > 0
        BackupSection.VAULTS -> vaultCount > 0
    }
}

/** Lightweight per-vault summary used to drive per-vault selection in the import UI. */
data class VaultSummary(
    val uuid: String,
    val name: String,
    val mediaCount: Int
)

/** Callback invoked as a backup/restore progresses through a [BackupSection]. */
typealias BackupProgressListener = (section: BackupSection, current: Int, total: Int) -> Unit
