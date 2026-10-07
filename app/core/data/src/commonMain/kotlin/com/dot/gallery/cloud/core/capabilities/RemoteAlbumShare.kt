/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core.capabilities

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.domain.model.Media
import kotlinx.serialization.Serializable

/** One collaborator entry for [RemoteAlbumWriteProvider.updateAlbumUsers]. */
@Serializable
data class RemoteAlbumShare(
    val userId: String,
    val role: RemoteAlbumShareRole
)