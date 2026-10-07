/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

/**
 * Metadata of a single vault media item. Mirrors [com.dot.gallery.domain.model.EncryptedMedia2].
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
    val fileName: String,
    /** Plaintext payload size in the archive. Required for schema v2 and newer. */
    val archiveSize: Long = -1L,
    /** Lower-case SHA-256 of the plaintext archive payload. Required for schema v2 and newer. */
    val sha256: String = ""
)
