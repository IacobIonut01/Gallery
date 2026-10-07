/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/** A vault and the metadata of all media it contains. Binary content is stored separately in the archive. */
@Serializable
data class VaultEntry(
    val uuid: String,
    val name: String,
    val media: List<VaultMediaEntry> = emptyList()
)
