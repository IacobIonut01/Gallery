/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import android.net.Uri

/**
 * Config backup/restore engine. The implementation lives in `:app`
 * (`ConfigBackupManagerImpl`); feature modules consume this surface.
 */
interface ConfigBackupManager {

    data class ExportResult(
        val settingsCount: Int,
        val localFavoritesCount: Int,
        val cloudFavoritesCount: Int,
        val cloudConfigsCount: Int,
        val vaultCount: Int,
        val vaultMediaCount: Int
    )

    data class ImportIssue(
        val section: BackupSection,
        val item: String,
        val message: String
    )

    data class ImportResult(
        val settingsRestored: Int,
        val settingsSkipped: Int,
        val cloudFavoritesRestored: Int,
        val cloudFavoritesSkipped: Int,
        val cloudConfigsRestored: Int,
        val cloudConfigsSkipped: Int,
        val cloudConfigsRequiringAuthentication: Int,
        val vaultsRestored: Int,
        val vaultsSkipped: Int,
        val vaultMediaRestored: Int,
        val vaultMediaSkipped: Int,
        /** Device media URIs that match backed-up local favorites and need a system consent to favorite. */
        val pendingLocalFavoriteUris: List<Uri>,
        /** Non-fatal item-level failures. A successful result may still be partial when this is non-empty. */
        val issues: List<ImportIssue>
    )

    suspend fun exportBackup(
        destination: Uri,
        selection: BackupSelection = BackupSelection(),
        password: String? = null,
        onProgress: BackupProgressListener = { _, _, _ -> }
    ): Result<ExportResult>

    suspend fun isEncryptedBackup(source: Uri): Boolean

    suspend fun inspectBackup(
        source: Uri,
        password: String? = null
    ): Result<BackupContents>

    suspend fun importBackup(
        source: Uri,
        selection: BackupSelection = BackupSelection(),
        password: String? = null,
        onProgress: BackupProgressListener = { _, _, _ -> }
    ): Result<ImportResult>
}
