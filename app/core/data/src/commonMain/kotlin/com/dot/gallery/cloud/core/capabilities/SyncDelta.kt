/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.domain.model.Media
import com.dot.gallery.core.util.or


/**
 * Result of a provider delta/index fetch.
 *
 * [items] are the assets that were added or changed since the watermark (or the whole
 * index for providers without a delta API). [deletedRemoteIds] carries ids the provider
 * reports as deleted since the watermark when it has a real deletion channel (e.g.
 * Immich's delta-sync endpoint). [completeRemoteIds] is non-null only when the provider
 * enumerated its ENTIRE remote index this run — callers must reconcile the local Room
 * cache against it (rows absent from the list are stale). It must stay null when the
 * enumeration was partial or aborted mid-scan, otherwise a transient error would wipe
 * the cache.
 */
data class SyncDelta(
    val items: List<CloudMediaEntity>,
    val deletedRemoteIds: List<String> = emptyList(),
    val completeRemoteIds: List<String>? = null
) {
    /** Number of upserted + provider-reported deleted rows (pruned-by-index rows not included). */
    val changedCount: Int get() = items.size + deletedRemoteIds.size
}