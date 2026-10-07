/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.domain.model.Media
import com.dot.gallery.domain.uri.query


interface SmartSearchCapableProvider : MediaCapabilityProvider {
    suspend fun smartSearch(query: String): Result<List<Media>>

    /**
     * Server-side "more like this" search anchored at the provider's own asset
     * [remoteId] (e.g. Immich's queryAssetId). Only meaningful for media this
     * provider serves.
     */
    suspend fun smartSearchByAsset(remoteId: String): Result<List<Media>>
}
