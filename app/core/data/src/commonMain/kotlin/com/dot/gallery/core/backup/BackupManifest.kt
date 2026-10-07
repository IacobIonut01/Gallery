/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

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
        const val MIN_SUPPORTED_SCHEMA_VERSION = 1
        const val SCHEMA_VERSION = 2
        const val MANIFEST_NAME = "manifest.json"
        const val VAULTS_DIR = "vaults"

        fun requireSupportedSchema(schemaVersion: Int) {
            if (schemaVersion !in MIN_SUPPORTED_SCHEMA_VERSION..SCHEMA_VERSION) {
                throw UnsupportedBackupSchemaException(schemaVersion)
            }
        }
    }
}
