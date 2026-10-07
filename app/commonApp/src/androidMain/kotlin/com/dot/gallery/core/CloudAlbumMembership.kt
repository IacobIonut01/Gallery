/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core

import com.dot.gallery.cloud.core.CloudAlbum
import com.dot.gallery.cloud.core.CloudUri
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.core.cloudAlbumId
import com.dot.gallery.cloud.core.stableIdHash
import com.dot.gallery.domain.model.IgnoredAlbum
import com.dot.gallery.domain.model.Album
import com.dot.gallery.cloud.core.matches
import com.dot.gallery.data.entity.CloudMediaEntity
import com.dot.gallery.core.util.or
import com.dot.gallery.core.startup.toAlbum
import com.dot.gallery.domain.uri.path
import com.dot.gallery.domain.util.volume
import com.dot.gallery.domain.model.hiddenInTimeline


/**
 * Identity of a cloud media item — or of a cloud album — within one provider account.
 * `remoteId` may contain slashes for path-based providers (WebDAV/ownCloud/Nextcloud/SMB/NFS),
 * so it is always carried whole, never reduced to a path segment.
 */
internal data class CloudAlbumMemberId(
    val providerType: ProviderType,
    val serverConfigId: Long,
    val remoteId: String
)

internal const val UNSORTED_ALBUM_SENTINEL = "__unsorted__"

/** Album id of the virtual "unsorted" album exposed per remote provider type. */
internal fun unsortedCloudAlbumId(providerType: ProviderType): Long =
    CloudAlbum.CLOUD_ALBUM_ID_BASE - stableIdHash(UNSORTED_ALBUM_SENTINEL + providerType.name)

/**
 * Resolve which provider an "unsorted" virtual cloud album id belongs to.
 * Returns null when [albumId] is not an unsorted-cloud-album id.
 */
internal fun unsortedAlbumProviderType(albumId: Long): ProviderType? =
    ProviderType.remoteTypes().firstOrNull { unsortedCloudAlbumId(it) == albumId }

/**
 * Extract the cloud member key from a `cloud://` media URI string, or null when the URI is
 * not a cloud URI or carries no account (`cfg`) context. Items without an account can never
 * be matched to an album, so they are neither "sorted" nor hideable.
 */
internal fun cloudMemberKey(uriString: String): CloudAlbumMemberId? {
    val parsed = CloudUri.parse(uriString) ?: return null
    if (parsed.configId <= 0L) return null
    return CloudAlbumMemberId(parsed.providerType, parsed.configId, parsed.remoteId)
}

private data class CloudAlbumHideTarget(
    val id: Long,
    val key: CloudAlbumMemberId,
    val pathToThumbnail: String,
    val relativePath: String,
    val volume: String
)

/**
 * The fields [IgnoredAlbum] matching inspects, reproduced from [CloudAlbum.toAlbum] without
 * building an [com.dot.gallery.domain.model.Album] (its `android.net.Uri` field
 * cannot be constructed on the JVM, which keeps this pure and unit-testable).
 */
private fun CloudAlbum.hideTarget(): CloudAlbumHideTarget {
    val thumb = if (thumbnailAssetId != null) {
        val base = "cloud://${providerType.name}/$thumbnailAssetId?size=thumbnail"
        if (serverConfigId > 0L) "$base&cfg=$serverConfigId" else base
    } else ""
    val relativePath = "cloud/${providerType.name}"
    return CloudAlbumHideTarget(
        id = cloudAlbumId(providerType, serverConfigId, remoteId),
        key = CloudAlbumMemberId(providerType, serverConfigId, remoteId),
        pathToThumbnail = thumb,
        relativePath = relativePath,
        volume = thumb.substringBeforeLast("/").removeSuffix(relativePath.removeSuffix("/"))
    )
}

/**
 * Builds the predicate deciding whether a cloud media item is hidden from the unified
 * timeline (and favorites/trash) by [blacklistedAlbums], or null when nothing applies.
 *
 * Cloud media carries the constant `albumID` -500 (`CloudMediaEntity.CLOUD_ALBUM_ID`), so
 * [IgnoredAlbum.matchesMedia] can never match it; hiding has to resolve through album
 * membership instead. An [IgnoredAlbum] whose id, `albumIds` entry, or wildcard matches a
 * cloud album — gated on [IgnoredAlbum.hiddenInTimeline] — hides every member of that album.
 * An entry matching a provider's virtual "unsorted" album id hides that provider's media
 * that belongs to no album.
 */
internal fun hiddenCloudMediaPredicate(
    blacklistedAlbums: List<IgnoredAlbum>,
    cloudAlbums: List<CloudAlbum>,
    membersByAlbum: Map<CloudAlbumMemberId, Set<CloudAlbumMemberId>>
): ((CloudAlbumMemberId) -> Boolean)? {
    val timelineHidden = blacklistedAlbums.filter { it.hiddenInTimeline }
    if (timelineHidden.isEmpty()) return null

    val hiddenMemberKeys = HashSet<CloudAlbumMemberId>()
    for (album in cloudAlbums) {
        val target = album.hideTarget()
        val hidden = timelineHidden.any {
            it.matchesForTimeline(
                id = target.id,
                path = target.pathToThumbnail,
                relativePath = target.relativePath,
                volume = target.volume
            )
        }
        if (hidden) membersByAlbum[target.key]?.let(hiddenMemberKeys::addAll)
    }
    val hiddenUnsortedProviders = timelineHidden.flatMapTo(HashSet()) { ignored ->
        (listOf(ignored.id) + ignored.albumIds).mapNotNull(::unsortedAlbumProviderType)
    }
    if (hiddenMemberKeys.isEmpty() && hiddenUnsortedProviders.isEmpty()) return null

    val allMemberKeys: Set<CloudAlbumMemberId> = if (hiddenUnsortedProviders.isNotEmpty()) {
        membersByAlbum.values.flatten().toSet()
    } else emptySet()
    return { key ->
        key in hiddenMemberKeys ||
            (key.providerType in hiddenUnsortedProviders && key !in allMemberKeys)
    }
}
