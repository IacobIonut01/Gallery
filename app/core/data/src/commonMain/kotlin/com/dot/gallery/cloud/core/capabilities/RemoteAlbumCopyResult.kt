/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.domain.model.Media
import kotlinx.serialization.Serializable

@Serializable
data class RemoteAlbumCopyResult(
    val state: RemoteAlbumCopyState,
    val remoteId: String? = null,
    val message: String = "",
    val retryable: Boolean = false
) {
    val isComplete: Boolean
        get() = state == RemoteAlbumCopyState.COPIED ||
            state == RemoteAlbumCopyState.ALREADY_PRESENT
}