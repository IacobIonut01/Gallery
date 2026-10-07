/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** Lightweight per-vault summary used to drive per-vault selection in the import UI. */
data class VaultSummary(
    val uuid: String,
    val name: String,
    val mediaCount: Int
)
