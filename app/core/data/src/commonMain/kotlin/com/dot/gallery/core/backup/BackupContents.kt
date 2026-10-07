/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

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
