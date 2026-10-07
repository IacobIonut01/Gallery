/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

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
