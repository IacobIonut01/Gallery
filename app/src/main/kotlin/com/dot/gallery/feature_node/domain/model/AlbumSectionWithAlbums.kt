/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */
package com.dot.gallery.feature_node.domain.model

import androidx.compose.runtime.Stable

@Stable
data class AlbumSectionWithAlbums(
    val section: AlbumSection,
    val albums: List<Album>
) {
    val totalCount: Long get() = albums.sumOf { it.count }
    val totalSize: Long get() = albums.sumOf { it.size }
    val latestTimestamp: Long get() = albums.maxOfOrNull { it.timestamp } ?: 0L
}
