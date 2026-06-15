/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.feature_node.domain.model.Media

interface SmartSearchCapableProvider : MediaCapabilityProvider {
    suspend fun smartSearch(query: String): Result<List<Media>>
}
