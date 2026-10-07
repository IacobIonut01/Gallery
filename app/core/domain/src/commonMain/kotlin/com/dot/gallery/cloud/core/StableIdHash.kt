/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

import com.dot.gallery.domain.util.sha256
import com.dot.gallery.cloud.core.CloudAlbumIdentity
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.domain.model.Album
import com.dot.gallery.domain.model.Media


/**
 * Generates a stable, collision-resistant positive Long from a string.
 * Uses the first 8 bytes of a SHA-256 digest, masked to positive range.
 * Collision probability: ~1 in 4.6 × 10^18 (vs ~1 in 2 × 10^9 for hashCode).
 */
fun stableIdHash(input: String): Long {
    val digest = sha256(input.encodeToByteArray())
    var result = 0L
    for (i in 0 until 8) {
        result = (result shl 8) or (digest[i].toLong() and 0xFFL)
    }
    return result and 0x7FFFFFFFFFFFFFFFL // ensure positive
}

/**
 * Stable, collision-resistant id for a cloud media item, used as [com.dot.gallery.domain.model.Media.id].
 *
 * Two guarantees the plain [stableIdHash] cannot give on its own:
 *  - **Distinct from local media:** local MediaStore ids are always non-negative, so cloud ids
 *    are mapped into the negative range. A local and a cloud item can therefore never share an id.
 *  - **Distinct across providers:** the [providerType] is mixed into the hash input, so two
 *    providers that happen to reuse the same remote id resolve to different ids.
 *  - **Distinct across accounts:** the [serverConfigId] is mixed in too, so two accounts of the
 *    SAME provider type (e.g. two Immich servers) that happen to reuse the same remote id resolve
 *    to different ids — preventing a duplicate timeline LazyGrid key crash.
 *
 * This keeps the timeline LazyGrid key "media_<id>_<label>" globally unique even when local and
 * cloud media from multiple accounts are merged into a single list.
 */
fun cloudMediaId(providerType: ProviderType, serverConfigId: Long, remoteId: String): Long {
    val positive = stableIdHash("${providerType.name}/$serverConfigId/$remoteId")
    // Shift into the strictly-negative range (-1 .. Long.MIN_VALUE); avoids the 0 edge case.
    return -1L - positive
}

/**
 * Stable id for a cloud ALBUM, used as [com.dot.gallery.domain.model.Album.id].
 *
 * Mirrors [cloudMediaId]'s uniqueness guarantees: the [providerType] AND [serverConfigId] are
 * mixed into the hash so two providers/accounts that expose an album with the SAME [remoteId]
 * (e.g. a "Photos/" folder on both a WebDAV and an ownCloud account) resolve to DIFFERENT ids.
 * Without this the albums-grid LazyGrid key "cloud_<id>" collides and crashes with
 * "Key ... was already used".
 *
 * Kept in the album id namespace (offset from [CloudAlbum.CLOUD_ALBUM_ID_BASE]) so all existing
 * `albumId < 0` cloud-album checks keep working.
 */
fun cloudAlbumId(providerType: ProviderType, serverConfigId: Long, remoteId: String): Long =
    CloudAlbum.CLOUD_ALBUM_ID_BASE - stableIdHash("${providerType.name}/$serverConfigId/$remoteId")

fun CloudAlbumIdentity.matches(albumId: Long): Boolean =
    serverConfigId > 0L && remoteId.isNotBlank() &&
        cloudAlbumId(providerType, serverConfigId, remoteId) == albumId
