/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider
import com.dot.gallery.cloud.core.MemoryInfo
import com.dot.gallery.core.Resource
import kotlinx.coroutines.flow.Flow

interface MemoriesCapableProvider : MediaCapabilityProvider {
    fun getMemories(): Flow<Resource<List<MemoryInfo>>>
}
