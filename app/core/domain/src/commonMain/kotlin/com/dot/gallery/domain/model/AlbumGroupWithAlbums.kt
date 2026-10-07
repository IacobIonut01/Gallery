/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AlbumGroupWithAlbums(
    val group: AlbumGroup,
    val albums: List<Album>
) {
    val totalCount: Long get() = albums.sumOf { it.count }
    val totalSize: Long get() = albums.sumOf { it.size }
    val latestTimestamp: Long get() = albums.maxOfOrNull { it.timestamp } ?: group.timestamp
}
