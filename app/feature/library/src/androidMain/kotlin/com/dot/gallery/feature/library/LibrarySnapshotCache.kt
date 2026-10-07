/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature.library

import com.dot.gallery.core.startup.StartupCacheStamp
import com.dot.gallery.feature.library.ui.LibrarySnapshot

/**
 * Persistence seam for the cold-start library snapshot. The encrypted DataStore plumbing
 * lives app-side in `StartupMediaCache`; [LibraryContentSource] consumes this surface.
 */
interface LibrarySnapshotCache {

    /** Cached snapshot matching [privacyFingerprint], or null on miss/invalid. */
    suspend fun readLibrary(privacyFingerprint: String): LibrarySnapshot?

    /** Persists [snapshot] under [privacyFingerprint]; no-op when [stamp] is null. */
    suspend fun writeLibrary(
        stamp: StartupCacheStamp?,
        privacyFingerprint: String,
        snapshot: LibrarySnapshot,
    )

    /** The stamp the next cache write should be recorded against. */
    suspend fun currentStamp(): StartupCacheStamp?
}
