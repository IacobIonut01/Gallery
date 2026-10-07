/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.startup

import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.UriMedia

/**
 * Warm-start media snapshot consumed by the repository's first-frame loads.
 * Implemented by the app-side `StartupMediaCache` (which also conforms to the
 * feature/library `LibrarySnapshotCache` contract).
 */
interface StartupMediaSnapshotCache {

    suspend fun currentStamp(): StartupCacheStamp?

    suspend fun readMedia(): List<UriMedia>?

    suspend fun readAlbums(): List<Album>?

    suspend fun writeMedia(stamp: StartupCacheStamp?, media: List<UriMedia>)

    suspend fun writeAlbums(stamp: StartupCacheStamp?, albums: List<Album>)
}
