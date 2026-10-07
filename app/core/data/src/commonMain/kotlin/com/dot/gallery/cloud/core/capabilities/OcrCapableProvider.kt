/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.OcrResult
import com.dot.gallery.core.Resource
import com.dot.gallery.domain.model.Media
import kotlinx.coroutines.flow.Flow
import com.dot.gallery.domain.uri.query


interface OcrCapableProvider : MediaCapabilityProvider {
    suspend fun extractText(mediaId: Long): OcrResult?
    fun searchByText(query: String): Flow<Resource<List<Media>>>
}
