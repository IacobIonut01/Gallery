/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.MediaCapabilityProvider

/**
 * Read-only tag sync: the provider lists its tags and resolves which remote assets
 * carry each tag. Write-back (assigning tags to remote assets) is intentionally not
 * part of this capability.
 */
interface TagsCapableProvider : MediaCapabilityProvider {
    suspend fun getTags(): Result<List<CloudTagInfo>>

    /** Remote ids of the assets carrying [tagId]; implementations page internally. */
    suspend fun getTagAssetIds(tagId: String): Result<List<String>>
}
