/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** A cloud server configuration. Mirrors [com.dot.gallery.cloud.data.entity.CloudServerConfigEntity]. */
@Serializable
data class CloudConfigEntry(
    val providerType: String,
    val serverUrl: String,
    /** Source database id retained only to link records inside this backup. */
    val sourceConfigId: Long = 0L,
    /** Stable identity used to associate favorites with this source account during import. */
    val sourceAccountId: String = "",
    /** Credentials are omitted from new backups because Android Keystore ciphertext is not portable. */
    val requiresReauthentication: Boolean = true,
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
    val readOnlyMode: Boolean = false,
    val downloadRemoteEnabled: Boolean = false,
    val downloadVideos: Boolean = true,
    val uploadBasePath: String = "",
    val uploadVideosPath: String = ""
)
