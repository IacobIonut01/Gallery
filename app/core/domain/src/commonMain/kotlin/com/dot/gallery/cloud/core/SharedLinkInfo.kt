/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable

@Serializable
data class SharedLinkInfo(
    val id: String,
    val key: String,
    val type: String = "INDIVIDUAL",
    val description: String? = null,
    val expiresAt: Long? = null,
    val allowDownload: Boolean = true,
    val allowUpload: Boolean = false,
    val showMetadata: Boolean = true,
    val password: String? = null,
    val assetCount: Int = 0,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val createdAt: Long = 0L,
    val thumbnailAssetId: String? = null,
    val albumId: String? = null,
    val albumName: String? = null
) {
    val accountKey: String
        get() = "${providerType.name}/$serverConfigId/$id"

    fun shareUrl(baseUrl: String): String = "$baseUrl/share/$key"

    val displayTitle: String
        get() = when {
            type == "ALBUM" && !albumName.isNullOrBlank() -> albumName
            !description.isNullOrBlank() -> description
            else -> "$assetCount items"
        }

    val isAlbumLink: Boolean get() = type == "ALBUM"
}
