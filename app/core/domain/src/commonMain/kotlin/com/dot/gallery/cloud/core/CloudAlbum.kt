/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import kotlinx.serialization.Serializable
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.uri.emptyUri
import com.dot.gallery.domain.uri.parseUri

@Serializable
data class CloudAlbum(
    val remoteId: String,
    val providerType: ProviderType,
    val serverConfigId: Long,
    val name: String,
    val assetCount: Int,
    val thumbnailAssetId: String? = null,
    val isShared: Boolean = false,
    /** False when the album is shared *with* this account and owned by someone else. */
    val isOwned: Boolean = true,
    /** Display name/email of the owning user when known (shared-with-me albums). */
    val ownerName: String = "",
    /** This account's collaborator role — "viewer" | "editor"; empty when owned or unknown. */
    val shareRole: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val syncEnabled: Boolean = true
) {
    fun toAlbum(): Album {
        val thumbUri = if (thumbnailAssetId != null) {
            val base = "cloud://${providerType.name}/$thumbnailAssetId?size=thumbnail"
            parseUri(if (serverConfigId > 0L) "$base&cfg=$serverConfigId" else base)
        } else emptyUri
        return Album(
            id = cloudAlbumId(providerType, serverConfigId, remoteId),
            label = "$name (${providerType.displayName})",
            uri = thumbUri,
            pathToThumbnail = thumbUri.toString(),
            relativePath = "cloud/${providerType.name}",
            timestamp = updatedAt / 1000L,
            count = assetCount.toLong(),
            size = 0L,
            cloudIdentity = CloudAlbumIdentity(providerType, serverConfigId, remoteId),
            isCloudShared = isShared,
            isCloudOwned = isOwned,
            cloudShareRole = shareRole,
            cloudOwnerName = ownerName,
            cloudName = name
        )
    }

    companion object {
        const val CLOUD_ALBUM_ID_BASE = -1000L
    }
}
