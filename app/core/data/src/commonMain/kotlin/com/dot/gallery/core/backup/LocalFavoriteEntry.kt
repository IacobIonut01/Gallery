/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.backup

import kotlinx.serialization.Serializable

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
