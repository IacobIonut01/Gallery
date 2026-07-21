/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.decoder

import androidx.compose.runtime.mutableStateMapOf

/**
 * Session-scoped store of per-image RAW [RawDevelopParams]. Shared between the media viewer's
 * subsampling region decoder ([RawRegionDecoder]) and the Develop bottom sheet so tuning the develop
 * recipe live re-renders the zoomed image. State is intentionally NOT persisted — it resets when the
 * process dies. Keyed by [com.dot.gallery.feature_node.domain.model.Media.id].
 *
 * Backed by a Compose snapshot map, so reads inside composables recompose on change.
 */
object RawDevelopStore {

    private val params = mutableStateMapOf<Long, RawDevelopParams>()

    /** Current recipe for [mediaId] (defaults to neutral AUTO). Snapshot-aware in composition. */
    fun paramsFor(mediaId: Long): RawDevelopParams = params[mediaId] ?: RawDevelopParams.AUTO

    /** True when the user has tuned this image away from the default. */
    fun isModified(mediaId: Long): Boolean = params[mediaId] != null

    fun update(mediaId: Long, value: RawDevelopParams) {
        params[mediaId] = value
    }

    fun reset(mediaId: Long) {
        params.remove(mediaId)
    }
}
