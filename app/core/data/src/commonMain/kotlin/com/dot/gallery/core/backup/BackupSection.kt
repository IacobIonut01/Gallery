/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** The individual data categories handled during a backup / restore, used for per-section progress. */
enum class BackupSection {
    SETTINGS,
    LOCAL_FAVORITES,
    CLOUD_FAVORITES,
    CLOUD_CONFIGS,
    VAULTS
}
